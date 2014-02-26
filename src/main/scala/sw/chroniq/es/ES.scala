package sw.chroniq.es

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.Client
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.scalastuff.esclient.ESClient


/**
 * Created with IntelliJ IDEA.
 * User: olegsmetanin
 * Date: 25/02/14
 * Time: 00:59
 * To change this template use File | Settings | File Templates.
 */
object ES {
//  val client : Client =
//    nodeBuilder.node.client
  val tsclient = new TransportClient
  tsclient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

  val client: Client = tsclient

//
//  val asd = client.execute(client.prepareMultiSearch())
//      asd
}
