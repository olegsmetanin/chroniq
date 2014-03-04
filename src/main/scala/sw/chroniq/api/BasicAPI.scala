package sw.chroniq.api

import scala.Some
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import sw.platform.systems._
import sw.platform.api._
import sw.platform.db._
import sw.platform.web._

case class Film(code: String, title: String, did: Int, date_prod: com.github.nscala_time.time.Imports.LocalDate, kind: String, len: com.github.nscala_time.time.Imports.Period)

object Film {
  import scala.language.implicitConversions
  import com.github.mauricio.async.db.RowData

  implicit def rowToFilm(row: RowData): Film = {
    {
      Film(
        row("code").asInstanceOf[String],
        row("title").asInstanceOf[String],
        row("did").asInstanceOf[Int],
        row("date_prod").asInstanceOf[com.github.nscala_time.time.Imports.LocalDate],
        row("kind").asInstanceOf[String],
        row("len").asInstanceOf[com.github.nscala_time.time.Imports.Period]
      )
    }
  }
}





class NoSuchMethod extends PartialFunction[APIRequest, Future[APIResponse]] {

  def isDefinedAt(scope: APIRequest): Boolean = true

  def apply(scope: APIRequest) =
    Future(APIResponse(JSONResponse.error("No such method")))

}

class SimpleRequestHandler extends APIHandler("simpleRequest") {

  def apply(request:APIRequest) = {
    Future(APIResponse(JSONResponse.result("Simple response")))
  }

}

class SimpleRequestRUHandler extends APIHandler("simpleRURequest") {

  def apply(request:APIRequest) = {
    Future(APIResponse(JSONResponse.result("привет всем")))
  }

}

class GetFilmsRequestHandler extends APIHandler("getFilms") {

  import DAO._

  def apply(request:APIRequest) = {
    DBs("saas")
      .sendQuery("SELECT code, title, did, date_prod, kind, len FROM FILMS")
      .asListOf[Film]
      .map {
      f =>
        APIResponse(JSONResponse.result(f.toString))
    }

  }
}


class SQLRequestHandler extends APIHandler("sql") {

  import DAO._

  def apply(request:APIRequest) = {
    (request.json \ "sql").asOpt[String] match {
      case Some(sql) => {
        DBs("saas")
          .sendQuery(sql)
          .asListOf[Any]
          .map {
          v =>
            APIResponse(JSONResponse.result(v.toString))
        }
      }
      case _ => {
        Future(APIResponse(JSONResponse.error("No sql command")))
      }
    }

  }
}


class BroadcastRequestHandler extends APIHandler("broadcast") {

  def apply(request:APIRequest) = {
    val message = (request.json \ "message").toString
    request.workActor.socketWorkers.foreach(_ ! Broadcast(
      s"""
          {
             "type" : "broadcast",
             "message": $message
          }
          """))

    Future(APIResponse(JSONResponse.result("OK")))

  }
}

class StreamRequestHandler extends APIHandler("stream") {

  def apply(request:APIRequest) = {

    val snd = request.workActor.context.sender

    val res = for {
      socket <- request.params.get("socketid").asInstanceOf[Option[String]]
      stream <- (request.json \ "streamid").asOpt[String]
    } yield {

      val periodic = request.workActor.context.system.scheduler.schedule(1.seconds, 1.second) {
        val time = System.currentTimeMillis()
        snd ! SocketSend(socket,
          s"""
                {
                   "type" : "stream",
                   "streamid": "$stream",
                   "message": "Stream message $time"
                }
              """)
      }

      val stopper = request.workActor.context.system.scheduler.scheduleOnce(10.second) {
        periodic.cancel

        snd ! SocketSend(socket,
          s"""
                {
                   "type" : "stream",
                   "streamid": "$stream",
                   "end": "true"
                }
              """)
      }

      APIResponse(JSONResponse.result("OK"))
    }

    Future(res.getOrElse(APIResponse(JSONResponse.error("No streamId or request coming not from socket"))))

  }
}
