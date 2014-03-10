package sw.platform.es

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.client.Client



object ES {

  val tsclient = new TransportClient
  tsclient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

  val client: Client = tsclient

  def apply() = client

}
