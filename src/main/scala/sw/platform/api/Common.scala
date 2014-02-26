package sw.platform.api

import scala.Predef._
import play.api.libs.json.JsValue
import sw.infrastructure._
import scala.concurrent.Future


case class APIRequest(method: String, json:JsValue, params: Map[String, Any], workActor: WorkActor)

case class APIResponse(body:String, headers:Option[Map[String,String]] = None)


abstract class APIHandler(method:String) extends PartialFunction[APIRequest, Future[APIResponse]] {

  def isDefinedAt(x: APIRequest): Boolean = x.method == method

}
