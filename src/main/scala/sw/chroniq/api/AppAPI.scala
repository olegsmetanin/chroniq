package sw.chroniq.api

import sw.platform.api._

class AppAPI extends APISystem {

  def handlers = new SimpleRequestHandler orElse
    new GetFilmsRequestHandler orElse
    new SQLRequestHandler orElse
    new SimpleRequestRUHandler orElse
    new BroadcastRequestHandler orElse
    new StreamRequestHandler orElse
    new AddPOI orElse
    new SearchPOI orElse
    new SearchPOI2 orElse
    new CreateIndexes orElse
    new NoSuchMethod

}

