package sw.platform.systems

import akka.actor.{ActorSystem}

import java.net.ServerSocket
import java.lang.management.ManagementFactory
import java.io.File
import spray.http.MediaTypes._


object Vars {
  val sessionID = "SessionID"
}





trait GenSystem {
  val role: String

  def apply()(implicit system: ActorSystem)
}

object Utils {
  def freePort = {
    val server = new ServerSocket(0)
    val port = server.getLocalPort()
    server.close
    port
  }

  val debugMode = ManagementFactory.getRuntimeMXBean.getInputArguments.contains("-Xdebug")

  def loadStaticFile(path:String) = {
    val source = scala.io.Source.fromFile(path)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    byteArray
  }

  def allFiles(path:File):List[File] = {
    val parts=path.listFiles.toList.partition(_.isDirectory)
    parts._2 ::: parts._1.flatMap(allFiles)
  }

  val ext = Map("js" -> `application/javascript`, "html" ->  `text/html`, "" -> `text/html`)

  def cType(path:String) = {
    try {
      val fn = path.split("/").last
      val d=fn.split("\\.")
      val ename = if (d.length > 1) d.last else ""
      ext.getOrElse(ename, `text/html`)
    } catch {
      case e:Exception => `text/html`
    }
  }

}

