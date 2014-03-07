package sw.platform.utils

import spray.http.{ContentTypes, ContentType}
import spray.http.MediaTypes._
import spray.http.ContentType._
import java.io.File
import java.util.jar.JarFile
import collection.JavaConversions._

object WebUtils {

  val ext:Map[String, ContentType] = Map(
    "js" -> `application/javascript`,
    "html" ->  `text/html`)

  def cType(path:String):ContentType = {
      val fn = path.split("/").last
      val d=fn.split("\\.")
      val ename = if (d.length > 1) d.last else "html"
      ext.getOrElse(ename, ContentTypes.NoContentType)
  }

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

  lazy val templatesFromClass = {
    import scala.tools.nsc.io._

    val lst= Class.forName("sw.platform.utils.Templates").newInstance.asInstanceOf[WebTemplatesDef].list

    lst.map {
      e =>
        e._1 ->(e._2, Streamable.bytes(getClass().getClassLoader.getResourceAsStream(e._3)))
    }.toMap
  }

}
