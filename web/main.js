var topic = $("#topic");

map = new L.Map('map');

var osm = new L.TileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {minZoom: 8, maxZoom: 12, attribution: 'Map data © OpenStreetMap contributors'});

var lat = geoip_latitude();
var lon = geoip_longitude();

// start the map in South-East England
map.setView(new L.LatLng(lat, lon), 9);
map.addLayer(osm);


// Move map actions

map.on('moveend', onMapMove);

function onMapMove(e) {
    mapChanges.onNext({topic: topic.val(), bounds: map.getBounds(), zoom: map.getZoom()});
}

// Input topic key actions

var topicKeyups = Rx.Observable.fromEvent(topic, 'keyup')
    .map(function (e) {
        return e.target.value;
    })
    .filter(function (text) {
        return text.length > 2;
    });

var throttledTopicKeyups = topicKeyups
    .throttle(500 /* ms */);

var topickeys = throttledTopicKeyups.subscribe(
    function (x) {
        mapChanges.onNext({topic: x, bounds: map.getBounds(), zoom: map.getZoom()});
    },
    function (err) {
        console.log('Error: ' + err);
    },
    function () {
        console.log('Completed');
    });

// Search actions

function searchAction(criteria) {
    var data = {
        "method":"searchPOI",
        "zoom"   : criteria.zoom,
        "bounds" : [[criteria.bounds.getSouth(), criteria.bounds.getWest()], [criteria.bounds.getNorth(), criteria.bounds.getEast()]],
        "topic"   : criteria.topic
    };
    console.log(JSON.stringify(data))
    var promise = $.ajax(
        {
            url: "/api",
            type: "POST",
            data: JSON.stringify(data),
            dataType: "json",
            error: function (jqXHR, textStatus, errorThrown) {
                console.log(jqXHR, textStatus, errorThrown)
            }
        }).promise();

    return Rx.Observable.fromPromise(promise);
}

// Apply search too map

var mapChanges = new Rx.Subject();

var searchResult = mapChanges.flatMapLatest(searchAction);

searchResult.subscribe(function (data) {



    console.log('Next search: ',data);


}, function (e) {

});


mapChanges.onNext({topic: "", bounds: map.getBounds(), zoom: map.getZoom()});