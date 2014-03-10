package sw.chroniq

import sw.platform.api._
import sw.chroniq.jsonapi._
import sw.chroniq.pages._


class JSONAPIRoutes extends GenJSONAPIRoutes {

  def handlers = new SimpleRequestHandler("simpleRequest") orElse
    new GetFilmsRequestHandler("getFilms") orElse
    new SQLRequestHandler("sql") orElse
    new SimpleRequestRUHandler("simpleRURequest") orElse
    new BroadcastRequestHandler("broadcast") orElse
    new StreamRequestHandler("stream") orElse
    new AddPOI("addPOI") orElse
    new SearchPOI("searchPOI") orElse
    new CreateIndexes("createIndexes") orElse
    new NoSuchMethod

}


class PageRoutes extends GenPageRoutes {

  def handlers = (if (sw.platform.utils.Utils.isJar)
    new StaticPagesFromJar
  else
    new LiveReloadStaticPages) orElse
    new EventPage("/event") orElse
    new HelloPage("/hello") orElse
    new NoSuchPage

}