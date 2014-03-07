import sbt._
import Keys._

object Helpers {

  def generate = {
    import java.io.File

    def allFiles(path:File):List[File] = {
      val parts=path.listFiles.toList.partition(_.isDirectory)
      parts._2 ::: parts._1.flatMap(allFiles)
    }

    val extStr:Map[String, String] = Map(
      "js" -> "ContentType(`application/javascript`)",
      "html" ->  "ContentType(`text/html`)"
    )

    def scType(path:String):String = {
        val fn = path.split("/").last
        val d=fn.split("\\.")
        val ename = if (d.length > 1) d.last else "html"
        extStr.getOrElse(ename, "NoContentType")
    }

    val files = allFiles(new File("src/main/webapp/public"))

    val lst0 = files.map{ f =>
      val fname = f.getName
      val url = if (fname.endsWith(".html")) fname.dropRight(5) else fname
      "(\"/"+ url + "\","+ scType(fname) + ",\"public/"+fname+"\")"
    }

    val lst = (lst0 ::: List("(\"/\", ContentType(`text/html`), \"public/index.html\")")).mkString(",")

    println(lst)

    s"""
package sw.platform.utils

import spray.http.ContentType
import spray.http.MediaTypes._
import spray.http.ContentTypes._
import scala.tools.nsc.io._

class Templates extends WebTemplatesDef {
  val list = List(
    $lst
  )
}
    """
  }
}
