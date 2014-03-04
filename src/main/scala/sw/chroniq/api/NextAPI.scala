package sw.chroniq.api

import collection.JavaConversions._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.action.admin.indices.delete.{DeleteIndexRequest}
import org.elasticsearch.action.search.{MultiSearchResponse, SearchResponse, SearchType}
import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.common.unit.DistanceUnit
import sw.platform.api._
import sw.platform.web._
import sw.chroniq.es.ES
import org.scalastuff.esclient.ESClient
import org.elasticsearch.search.{SearchHits}
import sw.platform.web.JSONResponse
import sw.platform.api.APIResponse
import scala.Some
import sw.platform.api.APIRequest
import scala.util._



class AddPOI extends APIHandler("addPOI") {

  def apply(request: APIRequest) = {

    import collection.JavaConversions._

    val json = request.json
    val in_lat = (json \ "lat").asOpt[Double]
    val in_lon = (json \ "lon").asOpt[Double]
    val desc = (json \ "desc").as[String]

    val lat = in_lat.get

    val lon = in_lon.get

    val client = ES.client

    def createNewCluster(zoom: Int, lat: Double, lon: Double) = {
      client.execute(client.prepareIndex("poiclusters", "poi")
        .setSource(
        s"""
{
  "zoom": $zoom,
  "location": {
    "lat":$lat,
    "lon":$lon
  }
}
      """
      ).request())
    }

    def findNearestCluster(lat: Double, lon: Double, hits: SearchHits) = {

      hits.map {
        hit =>
          val source = hit.getSource
          val hit_lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
          val hit_lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")
          hit.getId -> (math.pow(hit_lat - lat, 2) + math.pow(hit_lon - lon, 2))
      }.toList.sortWith(_._2 > _._2).head._1

    }

    //http://wiki.openstreetmap.org/wiki/Zoom_levels

    val distances = for {

      zoom <- 0 to 18

      distance <- Some((156412 / math.pow(2, zoom)) * 128)

    } yield (zoom, distance)

    val multiSearchRequest = client.prepareMultiSearch

    distances foreach {
      zd =>
        val (zoom, distance) = zd
        multiSearchRequest.add(client.prepareSearch("poiclusters").setTypes("poi")
          .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
          .setQuery(
          filteredQuery(
            matchAllQuery(),
            andFilter(
              geoDistanceFilter("location").point(lat, lon).distance(distance, DistanceUnit.METERS),
              termFilter("zoom", zoom)
            )
          )
        )
          .setSize(100))

    }

    val multiSearchResponse = client.execute(multiSearchRequest.request)

    val arrayOfIdFuture = multiSearchResponse.flatMap {
      msr =>

        val listOfIdFuture = msr.getResponses.zipWithIndex.map {
          msrii =>
            val (msri, zoom) = msrii
            val hits = msri.getResponse.getHits
            if (hits.getTotalHits == 0) {
              // Create new cluster
              for {
                cluster <- createNewCluster(zoom, lat, lon)
              } yield cluster.getId
            } else {
              // Find nearest cluster
              Future(findNearestCluster(lat, lon, hits))
            }

        }.toList

        Future sequence listOfIdFuture
    }

    arrayOfIdFuture.map {
      ids =>
        val strOfId = ids.zipWithIndex.foldLeft("") {
          case (acc, (id, zoom)) =>
            acc + " \"z" + zoom.toString + "\": \"" + id + "\","
        }

        client.execute(client.prepareIndex("poi", "poi")
          .setSource(
          s"""
      {
        $strOfId
        "desc": "$desc",
        "location": {
          "lat":$lat,
          "lon":$lon
        }
      }
            """
        ).request)

    } map {
      res =>
        APIResponse(JSONResponse.result("OK"))
    }


  }

}

class SearchPOI extends APIHandler("searchPOI") {

  case class POI(id: String, lat: Double, lon: Double, desc: String)

  case class Cluster(id: String, lat: Double, lon: Double, size: Long, poi: List[POI])


  def apply(request: APIRequest) = {

    import scala.language.existentials
    import collection.JavaConversions._

    try {

      val json = request.json
      val zoom = (json \ "zoom").as[Int]
      val jsbounds = (json \ "bounds").as[Seq[Seq[Double]]]
      val topic = (json \ "topic").as[String]




      val (southWestLat, southWestLon, northEastLat, northEastLon) = jsbounds.flatten match {
        case List(q, w, e, r, _*) => (q, w, e, r)
      }


      val client = ES.client

      val clustersResponse = client.execute(client.prepareSearch("poiclusters").setTypes("poi")
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(filteredQuery(matchAllQuery(), andFilter(
        geoBoundingBoxFilter("location").bottomLeft(southWestLat, southWestLon).topRight(northEastLat, northEastLon),
        termFilter("zoom", zoom)
      )
      )
      ).request())


      val multiSearchResponse = clustersResponse.flatMap {
        sr =>
          val multiSearchRequest = client.prepareMultiSearch
          sr.getHits.map {
            h =>
              val clusterId = h.getId
              multiSearchRequest.add(
                client.prepareSearch("poi").setTypes("poi")
                  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                  .setQuery(//filteredQuery(matchAllQuery(), termFilter("z"+zoom,cluster_id))
                  boolQuery().must(matchQuery("z" + zoom, clusterId)).must(matchQuery("desc", topic))
                )
                  .addField("")
                  .setSize(100)

              )

          }
          client.execute(multiSearchRequest.request())
      }



      val idsAndSize = multiSearchResponse.map {
        multiresp =>

          multiresp.getResponses.map {
            resp =>
              val hits = resp.getResponse.getHits
              if (hits.getTotalHits < 9) {
                (hits.getTotalHits, hits.take(8).map(_.getId))
              } else {
                (0, Nil)
              }
          }
      }

      val idsResponse = idsAndSize.flatMap {
        idsAndSizeList =>
          val idsq = idsQuery()
          idsAndSizeList.map {
            el =>
              el._2.foreach(h => idsq.addIds(h))
          }

          client.execute(client.prepareSearch("poi").setTypes("poi")
            .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
            .setQuery(idsq).request())
      }

      val idPOI = idsResponse.map {
        response =>
          response.getHits.map {
            hit =>
              val source = hit.getSource
              val id = hit.getId
              val lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
              val lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")
              val desc = source.get("desc").asInstanceOf[String]
              id -> POI(id, lat, lon, desc)
          }.toMap
      }

      val clusters = for {

        idsMap <- idPOI

        searchResp <- clustersResponse

        sizes <- idsAndSize.mapTo[Array[(Long, Iterable[String])]]

      } yield {

        searchResp.getHits.zip(sizes).map {
          hits =>
            val (hit, (size, poiIds)) = hits

            val source = hit.getSource
            val id = hit.getId
            val lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
            val lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")


            Cluster(id, lat, lon, size, poiIds.map(idsMap(_)).toList)

        }

      }

      clusters.flatMap {
        ci =>

          val j = ci.map {
            c =>
              val id = c.id
              val lat = c.lat
              val lon = c.lon
              val size = c.size
              val jpoi = c.poi.map {
                p =>
                  val pid = p.id
                  val plat = p.lat
                  val plon = p.lon
                  val pdesc = p.desc


                  s"""
            {
              "id": "$pid",
              "lat": $plat,
              "lon": $plon,
              "desc": "$pdesc"

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

          Future(APIResponse(j))


      }

    } catch {
      case e: play.api.libs.json.JsResultException => {
        Future(APIResponse(JSONResponse.error(e.toString)))
      }

    }
  }
}


class SearchPOI2 extends APIHandler("searchPOI2") {

  case class POI(id: String, lat: Double, lon: Double, desc: String)

  case class Cluster(id: String, lat: Double, lon: Double, size: Long, poi: List[POI])

  case class Params(zoom: Int, southWestLat: Double, southWestLon: Double, northEastLat: Double, northEastLon: Double, topic: String)

  def apply(request: APIRequest) = {


    import collection.JavaConversions._
    import org.elasticsearch.client.Client

    val p = Promise[APIResponse]

    implicit val client = ES.client

    parseInput(request) match {
      case Failure(e) => p success APIResponse(JSONResponse.error("error in params"))
      case Success(params) => {
        queryClusters(params) onComplete {
          case Failure(e) => p success APIResponse(JSONResponse.error("server error"))
          case Success(clustersResponse) => {
            queryClustersPOI(clustersResponse, params) match {
              case None => p success APIResponse(generateJson(List())) // Empty list of clusters
              case Some(queryIdOfPOI) => {
                queryIdOfPOI onComplete {
                  case Failure(e) => p success APIResponse(JSONResponse.error("server error"))
                  case Success(idOfPOIResponse) => {

                    val idsAndSize = getIdsAndSize(idOfPOIResponse)

                    queryIds(idsAndSize) onComplete {
                      case Failure(e) => p success APIResponse(JSONResponse.error("server error"))
                      case Success(idsResponse) => {
                        val POIMap = queryIdsToPOIMap(idsResponse)
                        val clusters = parseCluster(clustersResponse, idsAndSize, POIMap)
                        val json = generateJson(clusters)
                        p success APIResponse(json)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }



    def parseInput(request: APIRequest) = {
      import scala.language.existentials

      try {
        val json = request.json
        val zoom = (json \ "zoom").as[Int]
        val jsbounds = (json \ "bounds").as[Seq[Seq[Double]]]
        val (southWestLat, southWestLon, northEastLat, northEastLon) = jsbounds.flatten match {
          case List(q, w, e, r, _*) => (q, w, e, r)
        }
        val topic = (json \ "topic").as[String]
        Success(Params(zoom, southWestLat, southWestLon, northEastLat, northEastLon, topic))
      } catch {
        case e: Exception => Failure(e)
      }
    }


    def queryClusters(params: Params)(implicit client: Client) = client
      .execute(client.prepareSearch("poiclusters").setTypes("poi")
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


    def queryClustersPOI(searchResponse: SearchResponse, params: Params)(implicit client: Client) = {
      val multiSearchRequest = client.prepareMultiSearch
      val hits = searchResponse.getHits
      if (hits.getTotalHits == 0) {
        None
      } else {
        hits.map {
          h =>
            val clusterId = h.getId
            multiSearchRequest.add(
              client.prepareSearch("poi").setTypes("poi")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(
                boolQuery().must(matchQuery("z" + params.zoom, clusterId)).must(matchQuery("desc", params.topic))
              )
                .addField("")
                .setSize(100)
            )

        }
        Some(client.execute(multiSearchRequest.request()))
      }
    }

    def getIdsAndSize(multiresp: MultiSearchResponse) = {
      multiresp.getResponses.map {
        resp =>
          val hits = resp.getResponse.getHits
          if (hits.getTotalHits < 9) {
            (hits.getTotalHits, hits.take(8).map(_.getId))
          } else {
            (0L, Nil)
          }
      }
    }

    def queryIds(idsAndSizeList: Array[(Long, Iterable[String])])(implicit client: Client) = {
      val idsq = idsQuery()
      idsAndSizeList.map {
        el =>
          el._2.foreach(h => idsq.addIds(h))
      }

      client.execute(client.prepareSearch("poi").setTypes("poi")
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(idsq).request())
    }

    def queryIdsToPOIMap(response: SearchResponse) = {
      response.getHits.map {
        hit =>
          val source = hit.getSource
          val id = hit.getId
          val lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
          val lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")
          val desc = source.get("desc").asInstanceOf[String]
          id -> POI(id, lat, lon, desc)
      }.toMap
    }

    def parseCluster(searchResponse: SearchResponse, idsAndSizeList: Array[(Long, Iterable[String])], POIMap: Map[String, POI]) = {
      searchResponse.getHits.zip(idsAndSizeList).map {
        hits =>
          val (hit, (size, poiIds)) = hits

          val source = hit.getSource
          val id = hit.getId
          val lat = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lat")
          val lon = source.get("location").asInstanceOf[java.util.HashMap[String, Double]].get("lon")


          Cluster(id, lat, lon, size, poiIds.map(POIMap(_)).toList)

      }
    }


    def generateJson(clusters: Iterable[Cluster]) = {
      clusters.map {
        c =>
          val id = c.id
          val lat = c.lat
          val lon = c.lon
          val size = c.size
          val jpoi = c.poi.map {
            p =>
              val pid = p.id
              val plat = p.lat
              val plon = p.lon
              val pdesc = p.desc


              s"""
            {
              "id": "$pid",
              "lat": $plat,
              "lon": $plon,
              "desc": "$pdesc"

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


    p.future
  }
}

class CreateIndexes extends APIHandler("createIndexes") {

  def apply(request: APIRequest) = {

    val client = ES.client

    val deletePOIIndex = client.execute(new DeleteIndexRequest("poi"))

    val deleteClusterIndex = client.execute(new DeleteIndexRequest("poiclusters"))

    val createPOIIndex = client.execute(
      client.admin().indices().prepareCreate("poi").addMapping("poi",
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
      "desc": {"type": "string"},
      "location": {"type": "geo_point"}
    }
  }
}
        """
      ).request()
    )

    val createClusterIndex = client.execute(
      client.admin().indices().prepareCreate("poiclusters").addMapping("poi",
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

    Future sequence List(deletePOIIndex, deleteClusterIndex, createPOIIndex, createClusterIndex) map {
      r => APIResponse(JSONResponse.result("OK"))
    }

  }
}