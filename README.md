chroniq
=======

Start using sbt:

./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" -Dweb.port=8080 -Dsocket.port=8081 "run supervisor websystem socketsystem"

./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" "run worksystem"


Build one jar

./sbt assembly


Start one jar using chroniq start script

./chroniq "target/scala-2.10/chroniqserver.jar" -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" -Dweb.port=8080 -Dsocket.port=8081 "run supervisor websystem socketsystem"

./chroniq "target/scala-2.10/chroniqserver.jar" -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" "run worksystem"


Sample requests

curl -XPOST -H "Content-Type: text/plain; charset=UTF-8" 'http://localhost/api' -d '{
      "method" : "createIndexes"
}'


curl -XPOST -H "Content-Type: text/plain; charset=UTF-8" 'http://localhost/api' -d '{
      "method" : "addPOI",
      "event_id":"qweasd1",
      "lat" : 5,
      "lon" : 5,
      "date": "2014-02-01",
      "icon": "vendor/mapicons/battlefield.png",
      "lang": "en",
      "title": "Event Title",
      "markup": "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      "tags": "lorem"
}'


curl -XPOST -H "Content-Type: text/plain; charset=UTF-8" 'http://localhost/api' -d '{
      "method" : "searchPOI",
      "zoom"   : 10,
      "bounds" : [[-10,-10],[10,10]],
      "from"   : "2014-01-01",
      "to"     : "2014-03-01",
      "tags"   : "lorem"
}'




