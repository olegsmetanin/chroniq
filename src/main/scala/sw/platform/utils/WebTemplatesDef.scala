package sw.platform.utils

import spray.http.ContentType

trait WebTemplatesDef {
  val list:List[(String, ContentType, String)]
}
