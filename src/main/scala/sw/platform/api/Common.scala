package sw.platform.api

import scala.Predef._
import scala.concurrent.Future
import play.api.libs.json.JsValue
import sw.platform.systems._
import spray.http.{StatusCodes, StatusCode, ContentTypes, ContentType}

case class JSONAPIRequest(method: String, json:JsValue, params: Map[String, Any], workActor: WorkActor, headers:scala.List[spray.http.HttpHeader] = Nil)

case class JSONAPIResponse(body:String, headers:Option[Map[String,String]] = None, status: StatusCode = StatusCodes.OK)

abstract class GenJSONAPIRoutes extends PartialFunction[JSONAPIRequest, Future[JSONAPIResponse]] {

  def handlers:PartialFunction[JSONAPIRequest, Future[JSONAPIResponse]]

  def apply(v1: JSONAPIRequest): Future[JSONAPIResponse] = handlers(v1)

  def isDefinedAt(x: JSONAPIRequest): Boolean = true

}

case class PageRequest(path: String, params: Map[String, Any])

case class PageResponse(body:Array[Byte], contentType:ContentType = ContentTypes.`application/octet-stream`, headers:List[(String,String)] = Nil, status: StatusCode = StatusCodes.OK)

abstract class GenPageRoutes extends PartialFunction[PageRequest, Future[PageResponse]] {

  def handlers:PartialFunction[PageRequest, Future[PageResponse]]

  def apply(v1: PageRequest): Future[PageResponse] = handlers(v1)

  def isDefinedAt(x: PageRequest): Boolean = true

}

