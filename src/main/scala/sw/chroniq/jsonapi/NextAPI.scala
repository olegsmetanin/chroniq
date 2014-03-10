package sw.chroniq.jsonapi

import scala.Some
import scala.util._
import collection.JavaConversions._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import org.elasticsearch.action.admin.indices.delete.{DeleteIndexRequest}
import org.elasticsearch.action.search._
import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.search.{SearchHit, SearchHits}
import org.scalastuff.esclient.ESClient

import sw.platform.api._
import sw.platform.web._
import sw.platform.es._
import sw.platform.db._
import sw.platform.db.DAO._
import sw.chroniq.model._
import spray.http.HttpHeaders

class AddPOI(method: String) extends PartialFunction[JSONAPIRequest, Future[JSONAPIResponse]] {

  def isDefinedAt(x: JSONAPIRequest): Boolean = x.method == method

  case class Params(event_id: String, lat: Double, lon: Double, event_date: com.github.nscala_time.time.Imports.LocalDate, icon: String, lang: String, title: String, markup: String, tags: String)

  def apply(request: JSONAPIRequest) = {

    val promise = Promise[JSONAPIResponse]

    parseInput(request) match {
      case Failure(e) => promise success JSONAPIResponse(JSONResponse.error("error in params"))
      case Success(params) => {
        val add = for {
          se2DB <- saveEventsToDB(params)
          sed2DB <- saveEventDescriptionToDB(params)
          qnc <- queryNearestClusters(params, distances)
          ids <- createNewOrFindNeareastClusters(params, qnc)
          se2ES <- saveToES(params, ids)
        } yield {
          JSONAPIResponse("{\"result\":\"OK\"}")
        }

        add onComplete {
          case Success(resp) => promise success resp
          case Failure(e) => {
            println(e.toString)
            promise success JSONAPIResponse(JSONResponse.error("query error"))
          }
        }
      }
    }

    def parseInput(request: JSONAPIRequest) = {
      try {
        val json = request.json
        val in_event_id = (json \ "event_id").as[String]
        val in_lat = (json \ "lat").as[Double]
        val in_lon = (json \ "lon").as[Double]
        val in_date = (json \ "date").as[com.github.nscala_time.time.Imports.LocalDate]
        val in_icon = (json \ "icon").as[String]
        val in_lang = (json \ "lang").as[String]
        val in_title = (json \ "title").as[String]
        val in_markup = (json \ "markup").as[String]
        val in_tags = (json \ "tags").as[String]
        Success(Params(in_event_id, in_lat, in_lon, in_date, in_icon, in_lang, in_title, in_markup, in_tags))
      } catch {
        case e: Exception => Failure(e)
      }
    }

    def saveEventsToDB(params: Params) = {

      val sql = """
                  |INSERT INTO Event(
                  |            event_id, lat, lon, event_date, icon, tags)
                  |    VALUES (?, ?, ?, ?, ?, ? ) RETURNING id
                """.stripMargin
      DBs("events").sendPreparedStatement(
        sql, Array(params.event_id, params.lat, params.lon, params.event_date, params.icon, params.tags))
    }

    def saveEventDescriptionToDB(params: Params) = {

      val sql = """
                  |INSERT INTO EventDescription(
                  |            event_id, lang, title, markup, tags)
                  |    VALUES (?, ?, ?, ?, ?)
                """.stripMargin

      DBs("events").sendPreparedStatement(
        sql, Array(params.event_id, params.lang, params.title, params.markup, params.tags))
    }

    //http://wiki.openstreetmap.org/wiki/Zoom_levels
    def distances = for {

      zoom <- 0 to 18

      distance <- Some((156412 / math.pow(2, zoom)) * 128)

    } yield (zoom, distance)


    def queryNearestClusters(params: Params, zoomDistance: IndexedSeq[(Int, Double)]) = {

      val multiSearchRequest = ES().prepareMultiSearch

      zoomDistance foreach {
        zd =>
          val (zoom, distance) = zd
          multiSearchRequest.add(ES().prepareSearch("poiclusters").setTypes("poi")
            .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
            .setQuery(
              filteredQuery(
                matchAllQuery(),
                andFilter(
                  geoDistanceFilter("location").point(params.lat, params.lon).distance(distance, DistanceUnit.METERS),
                  termFilter("zoom", zoom)
                )
              )
            )
            .setSize(100))

      }

      ES().execute(multiSearchRequest.request)
    }


    def createNewOrFindNeareastClusters(params: Params, msr: MultiSearchResponse) = {

      val listOfIdFuture = msr.getResponses.zipWithIndex.map {
        msrii =>
          val (msri, zoom) = msrii
          val hits = msri.getResponse.getHits
          if (hits.getTotalHits == 0) {
            // Create new cluster
            for {
              cluster <- createNewCluster(zoom, params.lat, params.lon)
            } yield cluster.getId
          } else {
            // Find nearest cluster
            Future(findNearestCluster(params.lat, params.lon, hits))
          }

      }.toList

      Future sequence listOfIdFuture
    }

    def saveToES(params: Params, ids: List[String]) = {

      val strOfId = ids.zipWithIndex.foldLeft("") {
        case (acc, (id, zoom)) =>
          acc + " \"z" + zoom.toString + "\": \"" + id + "\","
      }

      val tags = params.tags
      val lat = params.lat
      val lon = params.lon

      ES().execute(ES().prepareIndex("poi", "poi", params.event_id)
        .setSource(
          s"""
          |{
          |  $strOfId
          |  "tags": "$tags",
          |  "location": {
          |    "lat":$lat,
          |    "lon":$lon
          |  }
          |}
          """.stripMargin
        ).request)
    }

    def createNewCluster(zoom: Int, lat: Double, lon: Double) = {
      ES().execute(ES().prepareIndex("poiclusters", "poi")
        .setSource(
          s"""
          |{
          |  "zoom": $zoom,
          |  "location": {
          |    "lat":$lat,
          |    "lon":$lon
          |  }
          |}
          """.stripMargin
        ).request())
    }

    def findNearestCluster(lat: Double, lon: Double, hits: SearchHits) = {
      val idD: Iterable[(String, Double)] = hits.map {
        hit =>
          val source = hit.getSource
          val hit_lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
          val hit_lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")
          (hit.getId -> (math.pow(hit_lat - lat, 2) + math.pow(hit_lon - lon, 2)))
      }
      idD.toList.sortWith(_._2 > _._2).head._1
    }

    promise.future
  }

}

class SearchPOI(method: String) extends PartialFunction[JSONAPIRequest, Future[JSONAPIResponse]] {

  def isDefinedAt(x: JSONAPIRequest): Boolean = x.method == method

  case class POI(event_id: String, lat: Double, lon: Double, icon: String, preview: String)

  case class Cluster(id: String, lat: Double, lon: Double, size: Long, poi: List[POI])

  case class Params(
                     zoom: Int,
                     southWestLat: Double,
                     southWestLon: Double,
                     northEastLat: Double,
                     northEastLon: Double,
                     from: Long,
                     to: Long,
                     tags: String,
                     lang: Option[Seq[String]]
                     )

  def apply(request: JSONAPIRequest) = {

    val promise = Promise[JSONAPIResponse]

    parseInput(request) match {
      case Failure(e) => promise success JSONAPIResponse(JSONResponse.error("error in params"))
      case Success(params) => {
        queryClusters(params) onComplete {
          case Failure(e) => {
            println(e)
            promise success JSONAPIResponse(JSONResponse.error("server error"))
          }
          case Success(clustersResponse) => {
            queryClustersPOI(clustersResponse, params) match {
              case None => promise success JSONAPIResponse(generateJson(List())) // Empty list of clusters
              case Some(queryIdOfPOI) => {
                queryIdOfPOI onComplete {
                  case Failure(e) => {
                    println(e)
                    promise success JSONAPIResponse(JSONResponse.error("server error"))
                  }
                  case Success(idOfPOIResponse) => {

                    val idsAndSize = getIdsAndSize(idOfPOIResponse)

                    val clasterFuture = for {
                      eMap <- queryEvent(idsAndSize)
                      edList <- queryEventDescription(idsAndSize)
                    } yield {
                      //println(eMap.toString())
                      //println(edList.toString())
                      parseCluster(params, clustersResponse, idsAndSize, eMap, edList)
                    }

                    clasterFuture onComplete {
                      case Failure(e) => {
                        println(e)
                        promise success JSONAPIResponse(JSONResponse.error("server error"))
                      }
                      case Success(clusters) => {
                        val json = generateJson(clusters)
                        //println(json)
                        promise success JSONAPIResponse(json)
                      }
                    }
                    //                    queryIds(idsAndSize) onComplete {
                    //                      case Failure(e) => p success JSONAPIResponse(JSONResponse.error("server error"))
                    //                      case Success(idsResponse) => {
                    //                        val POIMap = queryIdsToPOIMap(idsResponse)
                    //                        val clusters = parseCluster(clustersResponse, idsAndSize, POIMap)
                    //                        val json = generateJson(clusters)
                    //                        p success JSONAPIResponse(json)
                    //                      }
                    //                    }
                  }
                }
              }
            }
          }
        }
      }
    }



    def parseInput(request: JSONAPIRequest) = {
      import scala.language.existentials

      try {
        val json = request.json
        val zoom = (json \ "zoom").as[Int]
        val jsbounds = (json \ "bounds").as[Seq[Seq[Double]]]
        val (southWestLat, southWestLon, northEastLat, northEastLon) = jsbounds.flatten match {
          case List(q, w, e, r, _*) => (q, w, e, r)
        }
        val from = (json \ "from").as[Long]
        val to = (json \ "to").as[Long]
        val tags = (json \ "tags").as[String]
        val lang0 = request.headers.find(h => h.isInstanceOf[HttpHeaders.`Accept-Language`])
        val lang = lang0.map {
          h =>
            val hd = h.asInstanceOf[HttpHeaders.`Accept-Language`]
            hd.languages.map(lr => lr.value)
        }
        Success(Params(zoom, southWestLat, southWestLon, northEastLat, northEastLon, from, to, tags, lang))
      } catch {
        case e: Exception => Failure(e)
      }
    }


    def queryClusters(params: Params) = ES()
      .execute(ES().prepareSearch("poiclusters").setTypes("poi")
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(
        filteredQuery(
          matchAllQuery(),
          andFilter(
            geoBoundingBoxFilter("location")
              .bottomLeft(params.southWestLat, params.southWestLon).topRight(params.northEastLat, params.northEastLon),
            termFilter("zoom", params.zoom)
          )
        )
      ).request())

    def queryClustersPOI(searchResponse: SearchResponse, params: Params) = {
      val multiSearchRequest = ES().prepareMultiSearch
      val hits = searchResponse.getHits
      if (hits.getTotalHits == 0) {
        None
      } else {
        hits.map {
          h =>
            val clusterId = h.getId
            val bQuery0 = boolQuery().must(matchQuery("z" + params.zoom, clusterId))
            val bQuery1 = if (params.tags != "") bQuery0.must(matchQuery("tags", params.tags)) else bQuery0
            val bQuery2 = bQuery1.must(matchQuery("tags", params.tags))

            multiSearchRequest.add(
              ES().prepareSearch("poi").setTypes("poi")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(bQuery2)
                .addField("")
                .setSize(100)
            )

        }
        Some(ES().execute(multiSearchRequest.request()))
      }
    }

    def getIdsAndSize(multiresp: MultiSearchResponse) = {
      multiresp.getResponses.map {
        resp =>
          val hits = resp.getResponse.getHits
          if (hits.getTotalHits < 9) {
            val first8: Iterable[String] = hits.take(8).map {
              _.getId
            }
            (hits.getTotalHits, first8)
          } else {
            (hits.getTotalHits, Nil)
          }
      }.filter(_._1 != 0)
    }


    def queryEvent(idsAndSizeList: Array[(Long, Iterable[String])]) = {

      println(idsAndSizeList.toString)

      if (idsAndSizeList.length == 0) {

        Future(Map[String, Event]())

      } else {

        val lst = idsAndSizeList.map {
          el =>
            el._2.map("'" + _ + "'").mkString(",")
        }.mkString(",")

        val sql = s"""
          |SELECT * FROM Event WHERE event_id IN ($lst)
        """.stripMargin

        println(sql)

        DBs("events").sendQuery(
          sql).asMapOf[String, Event]("event_id")
      }
    }

    def queryEventDescription(idsAndSizeList: Array[(Long, Iterable[String])]) = {

      if (idsAndSizeList.length == 0) {

        Future(IndexedSeq[EventDescription]())

      } else {
        val lst = idsAndSizeList.map {
          el =>
            el._2.map("'" + _ + "'").mkString(",")
        }.mkString(",")

        val sql = s"""
          |SELECT * FROM EventDescription WHERE event_id IN ($lst)
        """.stripMargin

        println(sql)

        DBs("events").sendQuery(
          sql).asListOf[EventDescription]
      }
    }

    def parseCluster(params: Params, searchResponse: SearchResponse, idsAndSizeList: Array[(Long, Iterable[String])], eventMap: Map[String, Event], eventDescriptionList: IndexedSeq[EventDescription]) = {
      searchResponse.getHits.zip(idsAndSizeList).map {
        hits: (SearchHit, (Long, Iterable[String])) =>
          val (hit, (size, poiIds)) = hits

          val source = hit.getSource
          val id = hit.getId
          val clusterlat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
          val clusterlon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")


          val events = poiIds.map {
            eventMap(_)
          }
          val eventDescriptions = poiIds.map(pid => eventDescriptionList.filter(_.event_id == pid).head).toList
          val pois = events.zip(eventDescriptions).map {
            eed =>
              val (event, eventDescription) = eed
              POI(event.event_id, event.lat, event.lon, event.icon, eventDescription.title)
          }

          Cluster(id, clusterlat, clusterlon, size, pois.toList)

      }.toList
    }

    //    def queryIds(idsAndSizeList: Array[(Long, Iterable[String])]) = {
    //      val idsq = idsQuery()
    //      idsAndSizeList.map {
    //        el =>
    //          el._2.foreach(h => idsq.addIds(h))
    //      }
    //
    //      ES().execute(ES().prepareSearch("poi").setTypes("poi")
    //        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
    //        .setQuery(idsq).request())
    //    }
    //
    //
    //    def queryIdsToPOIMap(response: SearchResponse) = {
    //      response.getHits.map {
    //        hit =>
    //          val source = hit.getSource
    //          val id = hit.getId
    //          val lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
    //          val lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")
    //          val desc = source.get("desc").asInstanceOf[String]
    //          id -> POI(id, lat, lon, desc)
    //      }.toMap
    //    }
    //
    //    def parseCluster(searchResponse: SearchResponse, idsAndSizeList: Array[(Long, Iterable[String])], POIMap: Map[String, POI]) = {
    //      searchResponse.getHits.zip(idsAndSizeList).map {
    //        hits =>
    //          val (hit, (size, poiIds)) = hits
    //
    //          val source = hit.getSource
    //          val id = hit.getId
    //          val lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
    //          val lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")
    //
    //
    //          Cluster(id, lat, lon, size, poiIds.map(POIMap(_)).toList)
    //
    //      }
    //    }


    def generateJson(clusters: Iterable[Cluster]) = {
      clusters.map {
        c =>
          val id = c.id
          val lat = c.lat
          val lon = c.lon
          val size = c.size
          val jpoi = c.poi.map {
            p =>
              val pid = p.event_id
              val plat = p.lat
              val plon = p.lon
              val picon = p.icon
              val ppreview = p.preview


              s"""
            {
              "id": "$pid",
              "lat": $plat,
              "lon": $plon,
              "icon": "$picon",
              "preview": "$ppreview"

            }
            """

          } mkString("[", ",", "]")

          if (size != 0) {
            s"""
            {
              "id": "$id",
              "lat": $lat,
              "lon": $lon,
              "size": $size,
              "poi": $jpoi

            }
            """
          } else ""
      } filter (_ != "") mkString("{\"result\": { \"clusters\": [", ",", "]}}")
    }


    promise.future
  }
}

class CreateIndexes(method: String) extends PartialFunction[JSONAPIRequest, Future[JSONAPIResponse]] {

  def isDefinedAt(x: JSONAPIRequest): Boolean = x.method == method

  def apply(request: JSONAPIRequest) = {

    val deletePOIIndex = ES().execute(new DeleteIndexRequest("poi"))

    val deleteClusterIndex = ES().execute(new DeleteIndexRequest("poiclusters"))

    val createPOIIndex = ES().execute(
      ES().admin().indices().prepareCreate("poi").addMapping("poi",
        """
{
  "poi": {
    "properties": {
      "z0": {"type": "string"},
      "z1": {"type": "string"},
      "z2": {"type": "string"},
      "z3": {"type": "string"},
      "z4": {"type": "string"},
      "z5": {"type": "string"},
      "z6": {"type": "string"},
      "z7": {"type": "string"},
      "z8": {"type": "string"},
      "z9": {"type": "string"},
      "z10": {"type": "string"},
      "z11": {"type": "string"},
      "z12": {"type": "string"},
      "z13": {"type": "string"},
      "z14": {"type": "string"},
      "z15": {"type": "string"},
      "z16": {"type": "string"},
      "z17": {"type": "string"},
      "z18": {"type": "string"},
      "event_timestamp": {"type": "long"},
      "tags": {"type": "string"},
      "location": {"type": "geo_point"}
    }
  }
}
        """
      ).request()
    )

    val createClusterIndex = ES().execute(
      ES().admin().indices().prepareCreate("poiclusters").addMapping("poi",
        """
{
  "poi": {
    "properties": {
      "zoom": {"type": "integer"},
      "location": {"type": "geo_point"}
    }
  }
}
        """
      ).request()
    )

    val promise = Promise[JSONAPIResponse]

    Future sequence List(deletePOIIndex, deleteClusterIndex) onComplete {
      case Failure(e) => {
        println(e)
        promise success JSONAPIResponse(JSONResponse.error("delete indexes failed"))
      }
      case Success(s) => {
        Future sequence List(createPOIIndex, createClusterIndex) onComplete {
          case Failure(e) => promise success JSONAPIResponse(JSONResponse.error("create index failed"))
          case Success(s) => {
            promise success JSONAPIResponse(JSONResponse.result("OK"))
          }
        }
      }
    }

    promise.future
  }
}