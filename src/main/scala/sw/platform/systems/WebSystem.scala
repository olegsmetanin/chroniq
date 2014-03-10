package sw.platform.systems

import scala.util._
import scala.Predef._
import scala.language.implicitConversions
import scala.concurrent.duration._

import akka.actor._
import akka.pattern._
import akka.cluster.{Cluster}
import akka.io.IO

import spray.can.Http
import spray.http._
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import sw.platform.web._
import java.io.File

import spray.http.HttpCharsets._
import spray.http.HttpRequest
import akka.cluster.ClusterEvent.MemberUp
import spray.http.HttpResponse
import akka.actor.Terminated
import spray.http.Timedout
import sw.platform.utils.Utils
import sw.platform.utils.WebUtils._
import scala.Option
import akka.util.Timeout


object WebActor {
  val name = "webActor"
}

class WebActor extends Actor with ActorLogging {

  implicit val timeout: Timeout = 30.second // for the actor 'asks'

  import context.dispatcher

  // ExecutionContext for the futures and scheduler

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  var jobCounter = 0

  var workers = IndexedSeq.empty[WorkSystemInfo]




  def receive = {

    case _: Http.Connected => sender ! Http.Register(self)

    case msg@HttpRequest(_, uri, _, _, _) => {
      val snd = sender
      if (workers.size > 0) {
        jobCounter += 1
        val worker: ActorRef = workers(jobCounter % workers.size).actorRef
        (worker ? msg).mapTo[HttpResponse] onComplete {
          case Success(rsp) => snd ! rsp
          case Failure(e: akka.pattern.AskTimeoutException) => {
            if (uri == Uri.Path("/api")) {
              snd ! HttpResponse(entity = HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), JSONResponse.error("timeout")))
            } else {
              snd ! HttpResponse(status = 404, entity = "Request timeout")
            }
          }
          case Failure(e) => {
            snd ! HttpResponse(entity = HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), JSONResponse.error(e.toString)))
          }
        }
      } else {
        snd ! JSONHTTPResponse.NOWORKERS
      }
    }

    //case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")

    case Timedout(HttpRequest(method, uri, _, _, _)) => {
      // first look at haproxy timeouts
      sender ! HttpResponse(
        status = 500,
        entity = "The " + method + " request to '" + uri + "' has timed out..."
      )
    }


    case RegisterWorkSystem(name: String, version: String) if !workers.contains(WorkSystemInfo(name, version, sender)) => {
      println(Console.BLUE + "Web: worker " + sender.toString() + " " + name + " " + version + " REGISTRED" + Console.RESET)
      val newWorkSystem = WorkSystemInfo(name, version, sender)
      if (!workers.contains(newWorkSystem)) {
        context watch sender
        workers = workers :+ newWorkSystem
      }
    }

    case Terminated(actor) => {
      println(Console.RED + "Web: actor " + actor.toString() + " is Terminated" + Console.RESET)
      workers = workers.filterNot(_.actorRef == actor)
    }

  }

}

object WebSystem extends GenSystem {

  val role = "websystem"

  def apply()(implicit system: ActorSystem) = new WebSystem(system)

}

class WebSystem(akkaSystem: ActorSystem) {

  implicit val system: ActorSystem = akkaSystem

  val config = system.settings.config

  val webActor = system.actorOf(Props[WebActor], name = WebActor.name)

  val host = "0.0.0.0"

  val port = if (config.hasPath("web.port")) config.getInt("web.port") else Utils.freePort

  IO(Http) ! Http.Bind(webActor, interface = host, port = port)

  println(s"WebSystem started on port $port")

}