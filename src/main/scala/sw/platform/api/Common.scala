package sw.platform.api

import scala.Predef._
import scala.concurrent.Future
import play.api.libs.json.JsValue

import sw.platform.systems._

abstract class APISystem extends PartialFunction[APIRequest, Future[APIResponse]] {

  def handlers:PartialFunction[APIRequest, Future[APIResponse]]

  def apply(v1: APIRequest): Future[APIResponse] = handlers(v1)

  def isDefinedAt(x: APIRequest): Boolean = true

}

case class APIRequest(method: String, json:JsValue, params: Map[String, Any], workActor: WorkActor)

case class APIResponse(body:String, headers:Option[Map[String,String]] = None)


abstract class APIHandler(method:String) extends PartialFunction[APIRequest, Future[APIResponse]] {

  def isDefinedAt(x: APIRequest): Boolean = x.method == method

}
