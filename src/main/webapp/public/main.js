var tags = $("#tags");

$("#hidedialog").on('click',function () {
    $('#mapwrap').toggle();
    $('#dialogwrap').toggle();
})

$(function () {
    var now = moment();
    var nowround = moment({y: now.year(), M: now.month(), d: now.date()});
    var yesterday = moment(nowround).subtract('days', 1);
    var tomorrow = moment(nowround).add('days', 1);

    $('#from').datepicker({
        format: "yyyy-mm-dd",
        todayBtn: "linked",
        calendarWeeks: true,
        autoclose: true,
        todayHighlight: true
    }).datepicker('setDate', new Date(yesterday));

    $('#to').datepicker({
        format: "yyyy-mm-dd",
        todayBtn: "linked",
        calendarWeeks: true,
        autoclose: true,
        todayHighlight: true
    }).datepicker('setDate', new Date(tomorrow));

});


var map = new L.Map('map');

var osm = new L.TileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {attribution: 'Map data © OpenStreetMap contributors'});

var markerLayer = new L.LayerGroup();

markerLayer.addTo(map);

var lat = geoip_latitude();
var lon = geoip_longitude();

// start the map in South-East England
map.setView(new L.LatLng(lat, lon), 9);
map.addLayer(osm);


// Move map actions

map.on('moveend', onMapMove);

function onMapMove(e) {
    mapChanges.onNext({tags: tags.val(), bounds: map.getBounds(), zoom: map.getZoom()});
}

// Input topic key actions

var topicKeyups = Rx.Observable.fromEvent(tags, 'keyup')
    .map(function (e) {
        return e.target.value;
    });
//    .filter(function (text) {
//        return text.length > 2;
//    });

var throttledTopicKeyups = topicKeyups
    .throttle(500 /* ms */);

var topickeys = throttledTopicKeyups.subscribe(
    function (x) {
        mapChanges.onNext({tags: x, bounds: map.getBounds(), zoom: map.getZoom()});
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
        "tags"   : criteria.tags
    };
    //console.log(JSON.stringify(data))

    $("#update").addClass("icon-refresh-animate");

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
    $("#update").removeClass("icon-refresh-animate");
    if (!data.error) {
    var clusters = data.result.clusters;
    var markersArray = [];
    for (var i=0; i<clusters.length; i++) {
        var cluster = clusters[i];
        if (cluster.size>3) {


        markersArray.push(
            L.marker([cluster.lat, cluster.lon], {
                icon: L.divIcon({
                    className: 'marker-cluster marker-cluster-medium',
                    html:'<div><span>'+cluster.size+'</span></div>',
                    iconSize: new L.Point(40, 40)
                })
            })

        )
        } else {
            var pois = cluster.poi;
            for (var j=0; j<pois.length; j++) {
                var poi = pois[j];
                markersArray.push(
                    L.marker([poi.lat, poi.lon], {
                        icon: L.divIcon({
                            className: 'event',
                            html:'<div><img src="'+poi.icon+'"/>'+'</div>',
                            iconSize: new L.Point(32, 37),
                            iconAnchor:   [16, 35]
                        })
                    })
                        .bindLabel(poi.preview)
                        .on("click", function() {
                            $('#mapwrap').toggle();
                            $('#dialogwrap').toggle();
                        })


                )
            }
        }
    }

    markerLayer.clearLayers();
    for (var i=0; i<markersArray.length; i++) {
        var layer = markersArray[i];
        markerLayer.addLayer(layer);
    }
    } else {
        markerLayer.clearLayers();
    }
    //console.log('Next search: ',data);


}, function (e) {

});


mapChanges.onNext({tags: "", bounds: map.getBounds(), zoom: map.getZoom()});