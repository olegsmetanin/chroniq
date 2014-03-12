package sw.chroniq.tests

import org.scalatest._
import org.scalatest.Matchers._
import org.scalastuff.esclient.ESClient
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by olegsmetanin on 12/03/14.
 */

class AddPOISpec extends FlatSpec with ElasticSugar {

  "AddPOI" should "add records to elasticsearch" in {

    val result = ES().execute(ES().prepareIndex("poi", "poi", "1")
      .setSource(
        s"""
          |{
          |  "tags": "qwe",
          |  "location": {
          |    "lat":10,
          |    "lon":10
          |  }
          |}
          """.stripMargin
      ).request)

    Await.result(result,10.seconds).getId should be ("1")

  }
}