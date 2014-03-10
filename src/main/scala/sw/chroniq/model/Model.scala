package sw.chroniq.model

/**
 * Created by olegsmetanin on 09/03/14.
 */

case class Event(id:Int, event_id:String, lat:Double, lon:Double, event_timestamp:Long, icon:String, tags:String)

object Event {

  import scala.language.implicitConversions
  import com.github.mauricio.async.db.RowData

  implicit def rowToEvent(row: RowData): Event = {
    {
      Event(
        row("id").asInstanceOf[Int],
        row("event_id").asInstanceOf[String],
        row("lat").asInstanceOf[Double],
        row("lon").asInstanceOf[Double],
        row("event_timestamp").asInstanceOf[Long],
        row("icon").asInstanceOf[String],
        row("tags").asInstanceOf[String]
      )
    }
  }
}

case class EventDescription(id:Int, event_id:String, lang:String, title:String, markup:String, tags:String)

object EventDescription {

  import scala.language.implicitConversions
  import com.github.mauricio.async.db.RowData

  implicit def rowToEventDescription(row: RowData): EventDescription = {
    {
      EventDescription(
        row("id").asInstanceOf[Int],
        row("event_id").asInstanceOf[String],
        row("lang").asInstanceOf[String],
        row("title").asInstanceOf[String],
        row("markup").asInstanceOf[String],
        row("tags").asInstanceOf[String]
      )
    }
  }
}
