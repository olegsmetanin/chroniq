package sw.chroniq.evolutions

import sw.platform.evolutions.Evolution
import sw.platform.db.DBs
import scala.concurrent._
import scala.concurrent.duration._
import sw.platform.es.ES
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import sw.platform.api.JSONAPIResponse
import scala.util.{Success, Failure}
import sw.platform.web.JSONResponse
import org.scalastuff.esclient.ESClient
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by olegsmetanin on 11/03/14.
 */
class EV1 extends Evolution {

  def up: Unit = {


    val upgradeEventDB = DBs("events").sendQuery(
      """
        |
        |DROP TABLE Films;
        |
        |CREATE TABLE Films
        |(
        |  code character(5),
        |  title character varying(40),
        |  did integer,
        |  date_prod date,
        |  kind character varying(10),
        |  len interval hour to minute,
        |  CONSTRAINT production UNIQUE (date_prod)
        |);
        |
        |INSERT INTO Films(
        |            code, title, did, date_prod, kind, len)
        |    VALUES ('CODE1', 'Title1', 1, now(), 'Kind1', '00:40:00');
        |
        |
        |
        |DROP TABLE Event;
        |
        |CREATE TABLE Event
        |(
        |  id SERIAL,
        |  event_id varchar(255) NOT NULL,
        |  lat double precision NOT NULL,
        |  lon double precision NOT NULL,
        |  event_timestamp bigint NOT NULL,
        |  icon varchar(255) NOT NULL,
        |  tags text NOT NULL,
        |  PRIMARY KEY (id)
        |);
        |
        |DROP TABLE EventDescription;
        |
        |CREATE TABLE EventDescription
        |(
        |  id SERIAL,
        |  event_id varchar(255) NOT NULL,
        |  lang character(2) NOT NULL,
        |  title varchar(255) NOT NULL,
        |  markup text,
        |  tags text,
        |  PRIMARY KEY (id)
        |);
        |
        |
        |
      """.stripMargin)


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


    val promise = Promise[String]

    Future sequence List(deletePOIIndex, deleteClusterIndex) onComplete {
      case Failure(e) => {
        println(e)
      }
      case Success(s) => {
        Future sequence List(createPOIIndex, createClusterIndex) onComplete {
          case Failure(e) => println(e)
          case Success(s) => {
            promise success "OK"
          }
        }
      }
    }

    val upgrade = for {
      createDb <- upgradeEventDB
      createIndex <- promise.future
    } yield {
      "EV1 OK"
    }

    println(Await.result(upgrade, 100.seconds))

  }

  def down: Unit = {

  }


}
