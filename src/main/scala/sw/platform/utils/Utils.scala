package sw.platform.utils

import java.net.{ServerSocket}
import java.lang.management.ManagementFactory
import java.io.File
import spray.http.MediaTypes._
import collection.JavaConversions._
import spray.http.{ContentType, ContentTypes}
import java.util.jar.JarFile


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

  def allFilesFromJar(path:String):Map[String, (ContentType, Array[Byte])] = {
    import scala.tools.nsc.io.Streamable

    val jar = new JarFile(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()))
    jar.entries().filter(e => e.getName.startsWith(path) && !e.isDirectory).map { e =>
        "/"+e.getName.substring(path.length) -> (cType(e.getName), Streamable.bytes(getClass().getClassLoader.getResourceAsStream(e.getName)))
    }.toMap
  }

  val ext:Map[String, ContentType] = Map(
    "js" -> `application/javascript`,
    "html" ->  `text/html`,
    "" -> `text/html`)

  def cType(path:String):ContentType = {
    try {
      val fn = path.split("/").last
      val d=fn.split("\\.")
      val ename = if (d.length > 1) d.last else ""
      ext.getOrElse(ename, ContentTypes.NoContentType)
    } catch {
      case e:Exception => ContentTypes.NoContentType
    }
  }

  def isJar = {
    val className = getClass.getName.replace('.', '/')
    val classJar = getClass.getResource("/" + className + ".class").toString()
    classJar.startsWith("jar:")
  }

}
