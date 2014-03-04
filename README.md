chroniq
=======

./sbt -Dfile.encoding=UTF-8 -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" -Dweb.port=8080 -Dsocket.port=8081 "run supervisor websystem socketsystem"

./sbt -Dfile.encoding=UTF-8 -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" "run worksystem"


curl -XPOST -H "Content-Type: text/plain; charset=UTF-8" 'http://localhost/api' -d '{
      "method" : "createIndexes"
}'


curl -XPOST -H "Content-Type: text/plain; charset=UTF-8" 'http://localhost/api' -d '{
      "method" : "addPOI",
      "lat" : 5,
      "lon" : 5,
      "desc"   : "жизнь смерть война мир"
}'



curl -XPOST -H "Content-Type: text/plain; charset=UTF-8" 'http://localhost/api' -d '{
      "method" : "searchPOI",
      "zoom"   : 10,
      "bounds" : [[-10,-10],[10,10]],
      "topic"   : "жизнь"
}'

{
      "method" : "searchPOI2",
      "zoom"   : 9,
      "bounds":[[55.0,34],[56,40]],
      "topic"   : "жизнь"
}
