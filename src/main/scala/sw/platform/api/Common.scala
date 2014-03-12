package sw.platform.api

import scala.Predef._
import scala.concurrent.Future
import play.api.libs.json.JsValue
import sw.platform.systems._
import spray.http.{StatusCodes, StatusCode, ContentTypes, ContentType}

case class JSONAPIRequest(method: String, json:JsValue, params: Map[String, Any], workActor: WorkActor, headers:scala.List[spray.http.HttpHeader] = Nil)

case class JSONAPIResponse(body:String, headers:Option[Map[String,String]] = None, status: StatusCode = StatusCodes.OK)

case class JSONAPIStreamResponse(body:String, headers:Option[Map[String,String]] = None, status: StatusCode = StatusCodes.OK)

case class PageRequest(path: String, params: Map[String, Any])

case class PageResponse(body:Array[Byte], contentType:ContentType = ContentTypes.`application/octet-stream`, headers:List[(String,String)] = Nil, status: StatusCode = StatusCodes.OK)
