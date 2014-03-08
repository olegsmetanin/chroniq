package sw.platform.systems

import akka.actor._
import akka.actor.RootActorPath
import akka.cluster.{Member, MemberStatus, Cluster}
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.CurrentClusterState


import spray.http._
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import HttpCharsets._

import play.api.libs.json.Json

import sw.platform.api._
import sw.platform.utils.Utils
import sw.platform.utils.WebUtils._



import java.io.File
import scala.util._

import scala.util.Failure
import spray.http.HttpResponse
import scala.util.Success
import akka.actor.RootActorPath
import sw.platform.api.JSONAPIRequest
import spray.http.HttpRequest
import akka.cluster.ClusterEvent.MemberUp
import sw.platform.api.PageRequest
import akka.cluster.ClusterEvent.CurrentClusterState


object WorkActor {

  val name = "workActor"

}

class WorkActor(jsonapi: GenJSONAPIRoutes, pages: GenPageRoutes) extends Actor {


  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  var socketWorkers = IndexedSeq.empty[ActorRef]

  def receive = {

    case MemberUp(member) => {
      println(Console.YELLOW + "Worker: new member " + member.address + member.roles + " is up" + Console.RESET)
      register(member)
    }

    case state: CurrentClusterState => state.members.filter(_.status == MemberStatus.Up) foreach register

    case RegisterSocketSystem => {
      if (!socketWorkers.contains(sender)) {
        context watch sender
        socketWorkers = socketWorkers :+ sender
      }
    }

    case req@HttpRequest(GET, uri, _, _, _) => {

      import scala.concurrent.ExecutionContext.Implicits.global
      import spray.http.HttpHeaders._
      import scala.language.implicitConversions

      implicit def headers2SprayHeaders(in: List[(String, String)]): List[spray.http.HttpHeader] = {
        in.map {
          el =>
            new RawHeader(el._1, el._2)
        }
      }

      // important to save sender!!!
      val snd = sender

      val params: Map[String, Any] = Map[String, Any]("protocol" -> "http")
      val time = System.currentTimeMillis()
      pages(PageRequest(uri.path.toString(), params)).onComplete {
        case Success(resp) => {
          if ((System.currentTimeMillis() - time) > 10000) println("Request >10s :" + req.toString)
          val headers: List[spray.http.HttpHeader] = resp.headers
          val contentType = resp.contentType
          snd ! HttpResponse(entity = HttpEntity(contentType,resp.body), headers = headers)
        }
        case Failure(e) => {
          snd ! HttpResponse(entity = HttpEntity("Request failed"))
        }


      }
    }


    case req@HttpRequest(POST, Uri.Path("/api"), _, _, _) => {

      import scala.concurrent.ExecutionContext.Implicits.global

      // important to save sender!!!
      val snd = sender

      implicit val WorkActor = this

      val json = Json.parse(req.entity.data.asString)
      val method = (json \ "method").asOpt[String].get
      val params: Map[String, Any] = Map[String, Any]("protocol" -> "http")
      val time = System.currentTimeMillis()
      jsonapi(JSONAPIRequest(method, json, params, this)).onComplete {
        s =>
          if ((System.currentTimeMillis() - time) > 10000) println("Request >10s :" + req.toString)
          snd ! HttpResponse(entity = HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), s.get.body))
      }
    }

    case SocketRecieve(id: String, headers: Map[String, Set[String]], msg: String) => {
      import scala.concurrent.ExecutionContext.Implicits.global

      // important to save sender!!!
      val snd = sender
      val sid = id

      implicit val WorkActor = this

      val json = Json.parse(msg)
      val method = (json \ "method").asOpt[String].get
      val params: Map[String, Any] = Map[String, Any]("protocol" -> "socket", "socketid" -> id)

      jsonapi(JSONAPIRequest(method, json, params, this)).onComplete {
        s =>
          snd ! SocketSend(sid, s.get.body)
      }
    }

  }

  def register(member: Member): Unit = {
    if (member.hasRole(WebSystem.role))
      context.actorSelection(RootActorPath(member.address) / "user" / WebActor.name) ! RegisterWorkSystem(WorkSystem.name, WorkSystem.version)
    if (member.hasRole(SocketSystem.role))
      context.actorSelection(RootActorPath(member.address) / "user" / SocketActor.name) ! RegisterWorkSystem(WorkSystem.name, WorkSystem.version)
  }
}

object WorkSystem extends GenSystem {
  val role = "worksystem"
  val name = "worksystem"
  val version = "1"

  def apply()(implicit system: ActorSystem) = new WorkSystem(system)
}

class WorkSystem(system: ActorSystem) {

  val config = system.settings.config

  val jsonapiclass = if (config.hasPath("jsonapiroutes")) config.getString("jsonapiroutes") else throw new Exception("Failed to find jsonapiroutes property")

  val pageclass = if (config.hasPath("pageroutes")) config.getString("pageroutes") else throw new Exception("Failed to find pageroutes property")

  val api = Class.forName(jsonapiclass).newInstance.asInstanceOf[GenJSONAPIRoutes]

  val pages = Class.forName(pageclass).newInstance.asInstanceOf[GenPageRoutes]

  val actorProps = Props(classOf[WorkActor], api, pages)

  val workActor = system.actorOf(actorProps, name = WorkActor.name)

}
