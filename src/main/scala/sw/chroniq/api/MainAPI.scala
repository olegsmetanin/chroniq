package sw.api


object MainAPI {
  def apply() =
    new SimpleRequestHandler orElse
      new GetFilmsRequestHandler orElse
      new SQLRequestHandler orElse
      new BroadcastRequestHandler orElse
      new StreamRequestHandler orElse
      new AddPOI orElse
      new SearchPOI orElse
      new CreateIndexes orElse
      new NoSuchMethod
}
