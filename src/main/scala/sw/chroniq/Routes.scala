package sw.chroniq

import sw.platform.api._
import sw.chroniq.jsonapi._
import sw.chroniq.pages._
import scala.concurrent.Future

class JSONAPIRoutes extends PartialFunction[JSONAPIRequest, Future[JSONAPIResponse]] {

  override def isDefinedAt(x: JSONAPIRequest): Boolean = true

  override def apply(v1: JSONAPIRequest): Future[JSONAPIResponse] = {
    val handlers = {
      val h0 = new SimpleRequestHandler("simpleRequest") orElse
        new GetFilmsRequestHandler("getFilms") orElse
        new SQLRequestHandler("sql") orElse
        new SimpleRequestRUHandler("simpleRURequest") orElse
        new BroadcastRequestHandler("broadcast") orElse
        new StreamRequestHandler("stream") orElse
        new AddPOI("addPOI") orElse
        new SearchPOI("searchPOI")

      (if (sw.platform.utils.Utils.isJar)
        h0
      else
        h0 orElse
          new UpgradeDB("upgradeDB")) orElse
        new NoSuchMethod
    }
    handlers(v1)
  }
}


class PageRoutes extends PartialFunction[PageRequest, Future[PageResponse]] {

  def isDefinedAt(x: PageRequest): Boolean = true

  def handlers = (if (sw.platform.utils.Utils.isJar)
    new StaticPagesFromJar
  else
    new LiveReloadStaticPages) orElse
    new EventPage("/event") orElse
    new HelloPage("/hello") orElse
    new NoSuchPage

  def apply(v1: PageRequest): Future[PageResponse] = handlers(v1)

}