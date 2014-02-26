chroniq
=======

./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nod="akka.tcp://ClusterSystem@127.0.0.1:2552" -Dweb.port=8080 -Dsocket.port=8081 "run supervisor websystem socketsystem"

./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.1:2552" "run worksystem"


curl -XPOST 'http://localhost/api' -d '{
      "method" : "createIndexes"
}'


curl -XPOST 'http://localhost/api' -d '{
      "method" : "addPOI",
      "lat" : 5,
      "lon" : 5,
      "desc"   : "qweasd"
}'



curl -XPOST 'http://localhost/api' -d '{
      "method" : "searchPOI",
      "zoom"   : 10,
      "bounds" : [[-10,-10],[10,10]],
      "term"   : "qweasd"
}'

