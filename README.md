chroniq
=======


curl -XPOST 'http://localhost/api' -d '{
      "method" : "searchPOI",
      "zoom"   : 10,
      "bounds" : [[-10,-10],[10,10]],
      "term"   : "qweasd"
}'