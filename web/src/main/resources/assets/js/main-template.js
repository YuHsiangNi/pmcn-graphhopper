global.d3 = require('d3');
var Flatpickr = require('flatpickr');
require('flatpickr/dist/l10n');

var L = require('leaflet');
require('leaflet-contextmenu');
require('leaflet-loading');
var moment = require('moment');
require('./lib/leaflet.elevation-0.0.4.min.js');
require('./lib/leaflet_numbered_markers.js');

global.jQuery = require('jquery');
global.$ = global.jQuery;
require('./lib/jquery-ui-custom-1.12.0.min.js');
require('./lib/jquery.history.js');
require('./lib/jquery.autocomplete.js');

var ghenv = require("./config/options.js").options;
console.log(ghenv.environment);

var GHLocation = require('./graphhopper/GHLocation.js');
var GHCustom = require('./graphhopper/GHCustom.js');
var GHInput = require('./graphhopper/GHInput.js');
var GHRequest = require('./graphhopper/GHRequest.js');
var host = ghenv.routing.host;
if (!host) {
    if (location.port === '') {
        host = location.protocol + '//' + location.hostname;
    } else {
        host = location.protocol + '//' + location.hostname + ":" + location.port;
    }
}

var AutoComplete = require('./autocomplete.js');
if (ghenv.environment === 'development')
    var autocomplete = AutoComplete.prototype.createStub();
else
    var autocomplete = new AutoComplete(ghenv.geocoding.host, ghenv.geocoding.api_key);

var mapLayer = require('./map.js');
var nominatim = require('./nominatim.js');
var routeManipulation = require('./routeManipulation.js');
var gpxExport = require('./gpxexport.js');
var messages = require('./messages.js');
var translate = require('./translate.js');

var format = require('./tools/format.js');
var urlTools = require('./tools/url.js');
var vehicleTools = require('./tools/vehicle.js');
var tileLayers = require('./config/tileLayers.js');

var debug = false;
var ghRequest = new GHRequest(host, ghenv.routing.api_key);
var bounds = {};

var metaVersionInfo;

var path_point = [];
var path_snapped_waypoints = [];
var count =0;
var initCount =0;

var locationGet = true;
var locationStop = false;
var HomeCoordObject;
var PositionCoord;
var RoutingSize = [];

//var IntervalLocation;

// usage: log('inside coolFunc',this,arguments);
// http://paulirish.com/2009/log-a-lightweight-wrapper-for-consolelog/
if (global.window) {
    window.log = function () {
        log.history = log.history || [];   // store logs to an array for reference
        log.history.push(arguments);
        if (this.console && debug) {
            console.log(Array.prototype.slice.call(arguments));
        }
    };
}

$(document).ready(function (e) {
    // fixing cross domain support e.g in Opera
    jQuery.support.cors = true;

    gpxExport.addGpxExport(ghRequest);

    if (isProduction())
        $('#hosting').show();

    var History = window.History;
    if (History.enabled) {
        History.Adapter.bind(window, 'statechange', function () {
            // No need for workaround?
            // Chrome and Safari always emit a popstate event on page load, but Firefox doesn’t
            // https://github.com/defunkt/jquery-pjax/issues/143#issuecomment-6194330

            var state = History.getState();
            console.log(state);
            initFromParams(state.data, true);
        });
    }

    $('#locationform').submit(function (e) {
        // no page reload
        e.preventDefault();
        mySubmit();
    });

    /**web button click!!!**/
    //$("#gpx_test").click(function(){testGPX();});
    //$("#draw_line").click(function(){drawLine();});
    $("#clear_route").click(function(){ClearLayer();});
    $("#routing").click(function(){RoutingLocation();});
    $("#getLocation").click(function(){Location(locationGet)});
    $("#stopLocation").click(function(){Location(locationStop); StorageStayPlace();});
    $("#mapMatching").click(function () {MapMatching();});
    $("#queryFile").click(function () {displayFile();});
    $("#selectFile").click(function () {selectFile();});
    $("#exportFile").click(function () {ExportGPXFile();});
    $("#display_stay").click(function () {onlyDisplayTrajectoryLine(); DisplayStayPoint();});
    $("#trajectory").click(function () {DisplayGPXTrajectory();});
    $("#rawTrajectory").click(function(){DisplayFileRawTrajectory();});
    $("#expTrajectory").click(function () {ExperimentTrajectory();});

    var urlParams = urlTools.parseUrlWithHisto();
    $.when(ghRequest.fetchTranslationMap(urlParams.locale), ghRequest.getInfo())
            .then(function (arg1, arg2) {
                // init translation retrieved from first call (fetchTranslationMap)
                var translations = arg1[0];
                autocomplete.setLocale(translations.locale);
                ghRequest.setLocale(translations.locale);
                translate.init(translations);

                // init bounding box from getInfo result
                var json = arg2[0];
                var tmp = json.bbox;
                bounds.initialized = true;
                bounds.minLon = tmp[0];
                bounds.minLat = tmp[1];
                bounds.maxLon = tmp[2];
                bounds.maxLat = tmp[3];
                nominatim.setBounds(bounds);
                var vehiclesDiv = $("#vehicles");

                function createButton(vehicle, hide) {
                    var button = $("<button class='vehicle-btn' title='" + translate.tr(vehicle) + "'/>");
                    if (hide)
                        button.hide();

                    button.attr('id', vehicle);
                    button.html("<img src='img/" + vehicle + ".png' alt='" + translate.tr(vehicle) + "'></img>");
                    button.click(function () {
                        ghRequest.initVehicle(vehicle);
                        resolveAll();
                        routeLatLng(ghRequest);
                    });
                    return button;
                }

                if (json.features) {
                    ghRequest.features = json.features;

                    // car, foot and bike should come first. mc comes last
                    var prefer = {"car": 1, "foot": 2, "bike": 3, "motorcycle": 10000};
                    var showAllVehicles = urlParams.vehicle && (!prefer[urlParams.vehicle] || prefer[urlParams.vehicle] > 3);
                    var vehicles = vehicleTools.getSortedVehicleKeys(json.features, prefer);
                    if (vehicles.length > 0)
                        ghRequest.initVehicle(vehicles[0]);

                    if (ghRequest.isPublicTransit())
                        $(".time_input").show();

                    var hiddenVehicles = [];
                    for (var i in vehicles) {
                        var btn = createButton(vehicles[i].toLowerCase(), !showAllVehicles && i > 2);
                        vehiclesDiv.append(btn);

                        if (i > 2)
                            hiddenVehicles.push(btn);
                    }

                    if (!showAllVehicles && vehicles.length > 3) {
                        var moreBtn = $("<a id='more-vehicle-btn'> ...</a>").click(function () {
                            moreBtn.hide();
                            for (var i in hiddenVehicles) {
                                hiddenVehicles[i].show();
                            }
                        });
                        vehiclesDiv.append($("<a class='vehicle-info-link' href='https://graphhopper.com/api/1/docs/supported-vehicle-profiles/'>?</a>"));
                        vehiclesDiv.append(moreBtn);
                    }
                }
                metaVersionInfo = messages.extractMetaVersionInfo(json);

                mapLayer.initMap(bounds, setStartCoord, setIntermediateCoord, setEndCoord, setHomeCoord, setStayCoord, setPositionCoord, urlParams.layer, urlParams.use_miles);

                // execute query
                initFromParams(urlParams, true);

                checkInput();
            }, function (err) {
                console.log(err);
                $('#error').html('GraphHopper API offline? <a href="http://graphhopper.com/maps">Refresh</a>' + '<br/>Status: ' + err.statusText + '<br/>' + host);

                bounds = {
                    "minLon": -180,
                    "minLat": -90,
                    "maxLon": 180,
                    "maxLat": 90
                };
                nominatim.setBounds(bounds);
                mapLayer.initMap(bounds, setStartCoord, setIntermediateCoord, setEndCoord, setHomeCoord, urlParams.layer, urlParams.use_miles);
            });

    var language_code = urlParams.locale && urlParams.locale.split('-', 1)[0];
    if (language_code != 'en') {
        // A few language codes are different in GraphHopper and Flatpickr.
        var flatpickr_locale;
        switch (language_code) {
            case 'ca':  // Catalan
                flatpickr_locale = 'cat';
                break;
            case 'el':  // Greek
                flatpickr_locale = 'gr';
                break;
            default:
                flatpickr_locale = language_code;
        }
        if (Flatpickr.l10ns.hasOwnProperty(flatpickr_locale)) {
            Flatpickr.localize(Flatpickr.l10ns[flatpickr_locale]);
        }
    }

    $(window).resize(function () {
        mapLayer.adjustMapSize();
    });
    $("#locationpoints").sortable({
        items: ".pointDiv",
        cursor: "n-resize",
        containment: "parent",
        handle: ".pointFlag",
        update: function (event, ui) {
            var origin_index = $(ui.item[0]).data('index');
            sortable_items = $("#locationpoints > div.pointDiv");
            $(sortable_items).each(function (index) {
                var data_index = $(this).data('index');
                if (origin_index === data_index) {
                    //log(data_index +'>'+ index);
                    ghRequest.route.move(data_index, index);
                    if (!routeIfAllResolved())
                        checkInput();
                    return false;
                }
            });
        }
    });

    $('#locationpoints > div.pointAdd').click(function () {
        ghRequest.route.add(new GHInput());
        checkInput();
    });

    checkInput();
});

function initFromParams(params, doQuery) {
    ghRequest.init(params);

    var flatpickr = new Flatpickr(document.getElementById("input_date_0"), {
        defaultDate: new Date(),
        allowInput: true, /* somehow then does not sync!? */
        minuteIncrement: 15,
        time_24hr: true,
        enableTime: true
    });

    if (ghRequest.getEarliestDepartureTime()) {
        flatpickr.setDate(ghRequest.getEarliestDepartureTime());
    }

    var count = 0;
    var singlePointIndex;
    if (params.point)
        for (var key = 0; key < params.point.length; key++) {
            if (params.point[key] !== "") {
                count++;
                singlePointIndex = key;
            }
        }

    var routeNow = params.point && count >= 2;
    if (routeNow) {
        resolveCoords(params.point, doQuery);
    } else if (params.point && count === 1) {
        ghRequest.route.set(params.point[singlePointIndex], singlePointIndex, true);
        resolveIndex(singlePointIndex).done(function () {
            mapLayer.focus(ghRequest.route.getIndex(singlePointIndex), 15, singlePointIndex);
        });
    }
}


function resolveCoords(pointsAsStr, doQuery) {
    for (var i = 0, l = pointsAsStr.length; i < l; i++) {
        var pointStr = pointsAsStr[i];
        var coords = ghRequest.route.getIndex(i);
        if (!coords || pointStr !== coords.input || !coords.isResolved())
            ghRequest.route.set(pointStr, i, true);
    }

    checkInput();

    if (ghRequest.route.isResolved()) {
        resolveAll();
        routeLatLng(ghRequest, doQuery);
    } else {
        // at least one text input from user -> wait for resolve as we need the coord for routing
        $.when.apply($, resolveAll()).done(function () {
            routeLatLng(ghRequest, doQuery);
        });
    }
}

var FROM = 'from', TO = 'to';
function getToFrom(index) {
    if (index === 0)
        return FROM;
    else if (index === (ghRequest.route.size() - 1))
        return TO;
    return -1;
}

function checkInput() {
    var template = $('#pointTemplate').html(),
            len = ghRequest.route.size();

    // remove deleted points
    if ($('#locationpoints > div.pointDiv').length > len) {
        $('#locationpoints > div.pointDiv:gt(' + (len - 1) + ')').remove();
    }

    // properly unbind previously click handlers
    $("#locationpoints .pointDelete").off();

    var deleteClickHandler = function () {
        var index = $(this).parent().data('index');
        ghRequest.route.removeSingle(index);
        mapLayer.clearLayers();
        checkInput();
        routeLatLng(ghRequest, false);
    };

    // console.log("## new checkInput");
    for (var i = 0; i < len; i++) {
        var div = $('#locationpoints > div.pointDiv').eq(i);
        // console.log(div.length + ", index:" + i + ", len:" + len);
        if (div.length === 0) {
            $('#locationpoints > div.pointAdd').before(translate.nanoTemplate(template, {id: i}));
            div = $('#locationpoints > div.pointDiv').eq(i);
        }

        var toFrom = getToFrom(i);
        div.data("index", i);
        div.find(".pointFlag").attr("src",
                (toFrom === FROM) ? 'img/marker-small-green.png' :
                ((toFrom === TO) ? 'img/marker-small-red.png' : 'img/marker-small-blue.png'));
        if (len > 2) {
            div.find(".pointDelete").click(deleteClickHandler).prop('disabled', false).removeClass('ui-state-disabled');
        } else {
            div.find(".pointDelete").prop('disabled', true).addClass('ui-state-disabled');
        }

        autocomplete.showListForIndex(ghRequest, routeIfAllResolved, i);
        if (translate.isI18nIsInitialized()) {
            var input = div.find(".pointInput");
            if (i === 0)
                $(input).attr("placeholder", translate.tr("from_hint"));
            else if (i === (len - 1))
                $(input).attr("placeholder", translate.tr("to_hint"));
            else
                $(input).attr("placeholder", translate.tr("via_hint"));
        }
    }
}

function setToStart(e) {
    var latlng = e.relatedTarget.getLatLng(),
            index = ghRequest.route.getIndexByCoord(latlng);
    ghRequest.route.move(index, 0);
    routeIfAllResolved();
}

function setToEnd(e) {
    var latlng = e.relatedTarget.getLatLng(),
            index = ghRequest.route.getIndexByCoord(latlng);
    ghRequest.route.move(index, -1);
    routeIfAllResolved();
}

function setStartCoord(e) {
    ghRequest.route.set(e.latlng.wrap(), 0);
    resolveFrom();
    routeIfAllResolved();
}

function setIntermediateCoord(e) {
    var routeLayers = mapLayer.getSubLayers("route");
    var routeSegments = routeLayers.map(function (rl) {
        return {
            coordinates: rl.getLatLngs(),
            wayPoints: rl.feature.properties.snapped_waypoints.coordinates.map(function (wp) {
                return L.latLng(wp[1], wp[0]);
            })
        };
    });
    var index = routeManipulation.getIntermediatePointIndex(routeSegments, e.latlng);
    ghRequest.route.add(e.latlng.wrap(), index);
    resolveIndex(index);
    routeIfAllResolved();
    routeIfAllResolved();
}

function deleteCoord(e) {
    var latlng = e.relatedTarget.getLatLng();
    ghRequest.route.removeSingle(latlng);
    mapLayer.clearLayers();
    routeLatLng(ghRequest, false);
}

function setEndCoord(e) {
    var index = ghRequest.route.size() - 1;
    ghRequest.route.set(e.latlng.wrap(), index);
    resolveTo();
    routeIfAllResolved();
}

/**function e is event**/
function setHomeCoord(e) {
    HomeCoordObject = e.latlng.wrap();
    mapLayer.createMarkerHome(HomeCoordObject.lat,HomeCoordObject.lng);
    RoutingSize[1] = 1;


    if(RoutingSize[1] === 1 && RoutingSize[2] === 2){
        routing();
        RoutingSize[1] = 0;
        RoutingSize[2] = 0;
    }
    /* Transfer Server
    var GPXc = new GHCustom();
    GPXc.SetHomeNode(HomeCoordObject.lat,HomeCoordObject.lng);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        mapLayer.createMarkerHome(HomeCoordObject.lat,HomeCoordObject.lng);
    });*/
}

/**Set Stay Coords function**/
function setStayCoord(e) {
    var StayCoordObject = e.latlng.wrap();

    var GPXc = new GHCustom();
    GPXc.SetStayPlace(StayCoordObject.lat, StayCoordObject.lng);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        mapLayer.createMarkerStay(StayCoordObject.lat,StayCoordObject.lng);
    });
}

/**Set Now Position Coords function**/
function setPositionCoord(e) {
    PositionCoord = e.latlng.wrap();
    mapLayer.createMarkerGPX(PositionCoord.lat,PositionCoord.lng);
    RoutingSize[2] = 2;


    if(RoutingSize[1] === 1 && RoutingSize[2] === 2){
        routing();
        RoutingSize[1] = 0;
        RoutingSize[2] = 0;
    }

    /* Transfer Server
    var GPXc = new GHCustom();
    GPXc.SetNowPosition(PositionCoord.lat, PositionCoord.lng);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        mapLayer.createMarkerGPX(PositionCoord.lat,PositionCoord.lng);
    });
    */
}

function routeIfAllResolved(doQuery) {
    if (ghRequest.route.isResolved()) {
        routeLatLng(ghRequest, doQuery);
        return true;
    }
    return false;
}

function setFlag(coord, index) {
    if (coord.lat) {
        var toFrom = getToFrom(index);
        // intercept openPopup
        var marker = mapLayer.createMarker(index, coord, setToEnd, setToStart, deleteCoord, ghRequest);
        marker._openPopup = marker.openPopup;
        marker.openPopup = function () {
            var latlng = this.getLatLng(),
                    locCoord = ghRequest.route.getIndexFromCoord(latlng),
                    content;
            if (locCoord.resolvedList && locCoord.resolvedList[0] && locCoord.resolvedList[0].locationDetails) {
                var address = locCoord.resolvedList[0].locationDetails;
                content = format.formatAddress(address);
                // at last update the content and update
                this._popup.setContent(content).update();
            }
            this._openPopup();
        };
        var _tempItem = {
            text: translate.tr('set_start'),
            icon: './img/marker-small-green.png',
            callback: setToStart,
            index: 1
        };
        if (toFrom === -1)
            marker.options.contextmenuItems.push(_tempItem); // because the Mixin.ContextMenu isn't initialized
        marker.on('dragend', function (e) {
            mapLayer.clearLayers();
            // inconsistent leaflet API: event.target.getLatLng vs. mouseEvent.latlng?
            var latlng = e.target.getLatLng();
            autocomplete.hide();
            ghRequest.route.getIndex(index).setCoord(latlng.lat, latlng.lng);
            resolveIndex(index);
            // do not wait for resolving and avoid zooming when dragging
            ghRequest.do_zoom = false;
            routeLatLng(ghRequest, false);
        });
    }
}

function resolveFrom() {
    return resolveIndex(0);
}

function resolveTo() {
    return resolveIndex((ghRequest.route.size() - 1));
}

function resolveIndex(index) {
    setFlag(ghRequest.route.getIndex(index), index);
    if (index === 0) {
        if (!ghRequest.to.isResolved())
            mapLayer.setDisabledForMapsContextMenu('start', true);
        else
            mapLayer.setDisabledForMapsContextMenu('start', false);
    } else if (index === (ghRequest.route.size() - 1)) {
        if (!ghRequest.from.isResolved())
            mapLayer.setDisabledForMapsContextMenu('end', true);
        else
            mapLayer.setDisabledForMapsContextMenu('end', false);
    }

    return nominatim.resolve(index, ghRequest.route.getIndex(index));
}

function resolveAll() {
    var ret = [];
    for (var i = 0, l = ghRequest.route.size(); i < l; i++) {
        ret[i] = resolveIndex(i);
    }

    if(ghRequest.isPublicTransit())
        ghRequest.setEarliestDepartureTime(
            moment($("#input_date_0").val(), 'YYYY-MM-DD HH:mm').toISOString());

    return ret;
}

function flagAll() {
    for (var i = 0, l = ghRequest.route.size(); i < l; i++) {
        setFlag(ghRequest.route.getIndex(i), i);
    }
}

function routeLatLng(request, doQuery) {
    var i;

    // do_zoom should not show up in the URL but in the request object to avoid zooming for history change
    var doZoom = request.do_zoom;
    request.do_zoom = true;

    var urlForHistory = request.createHistoryURL() + "&layer=" + tileLayers.activeLayerName;

    // not enabled e.g. if no cookies allowed (?)
    // if disabled we have to do the query and cannot rely on the statechange history event
    if (!doQuery && History.enabled) {
        // 2. important workaround for encoding problems in history.js
        var params = urlTools.parseUrl(urlForHistory);
        console.log(params);
        params.do_zoom = doZoom;
        // force a new request even if we have the same parameters
        params.mathRandom = Math.random();
        History.pushState(params, messages.browserTitle, urlForHistory);
        return;
    }
    var infoDiv = $("#info");
    infoDiv.empty();
    infoDiv.show();
    var routeResultsDiv = $("<div class='route_results'/>");
    infoDiv.append(routeResultsDiv);

    mapLayer.clearElevation();
    mapLayer.clearLayers();
    flagAll();

    mapLayer.setDisabledForMapsContextMenu('intermediate', false);

    $("#vehicles button").removeClass("selectvehicle");
    $("button#" + request.getVehicle().toLowerCase()).addClass("selectvehicle");

    var urlForAPI = request.createURL();
    console.log(urlForAPI);
    routeResultsDiv.html('<img src="img/indicator.gif"/> Search Route ...');
    request.doRequest(urlForAPI, function (json) {
        console.log(json);
        routeResultsDiv.html("");
        if (json.message) {
            var tmpErrors = json.message;
            console.log(tmpErrors);
            if (json.hints) {
                for (var m = 0; m < json.hints.length; m++) {
                    routeResultsDiv.append("<div class='error'>" + json.hints[m].message + "</div>");
                }
            } else {
                routeResultsDiv.append("<div class='error'>" + tmpErrors + "</div>");
            }
            return;
        }

        function createClickHandler(geoJsons, currentLayerIndex, tabHeader, oneTab, hasElevation, useMiles) {
            return function () {

                var currentGeoJson = geoJsons[currentLayerIndex];
                mapLayer.eachLayer(function (layer) {
                    // skip markers etc
                    if (!layer.setStyle)
                        return;

                    var doHighlight = layer.feature === currentGeoJson;
                    layer.setStyle(doHighlight ? highlightRouteStyle : alternativeRouteStye);
                    if (doHighlight) {
                        if (!L.Browser.ie && !L.Browser.opera)
                            layer.bringToFront();
                    }
                });

                if (hasElevation) {
                    mapLayer.clearElevation();
                    mapLayer.addElevation(currentGeoJson, useMiles);
                }

                headerTabs.find("li").removeClass("current");
                routeResultsDiv.find("div").removeClass("current");

                tabHeader.addClass("current");
                oneTab.addClass("current");
            };
        }

        var headerTabs = $("<ul id='route_result_tabs'/>");
        if (json.paths.length > 1) {
            routeResultsDiv.append(headerTabs);
            routeResultsDiv.append("<div class='clear'/>");
        }

        // the routing layer uses the geojson properties.style for the style, see map.js
        var defaultRouteStyle = {color: "#00cc33", "weight": 5, "opacity": 0.6};
        var highlightRouteStyle = {color: "#00cc33", "weight": 6, "opacity": 0.8};
        var alternativeRouteStye = {color: "darkgray", "weight": 6, "opacity": 0.8};
        var geoJsons = [];
        var firstHeader;

        // Create buttons to toggle between SI and imperial units.
        var createUnitsChooserButtonClickHandler = function (useMiles) {
            return function () {
                mapLayer.updateScale(useMiles);
                ghRequest.useMiles = useMiles;
                resolveAll();
                routeLatLng(ghRequest);
            };
        };

        if(json.paths.length > 0 && json.paths[0].points_order) {
            mapLayer.clearLayers();
            var po = json.paths[0].points_order;
            for (i = 0; i < po.length; i++) {
                setFlag(ghRequest.route.getIndex(po[i]), i);
            }
        }

        for (var pathIndex = 0; pathIndex < json.paths.length; pathIndex++) {
            var tabHeader = $("<li>").append((pathIndex + 1) + "<img class='alt_route_img' src='img/alt_route.png'/>");
            if (pathIndex === 0)
                firstHeader = tabHeader;

            headerTabs.append(tabHeader);
            var path = json.paths[pathIndex];
            var style = (pathIndex === 0) ? defaultRouteStyle : alternativeRouteStye;

            var geojsonFeature = {
                "type": "Feature",
                "geometry": path.points,
                "properties": {
                    "style": style,
                    name: "route",
                    snapped_waypoints: path.snapped_waypoints
                }
            };

            geoJsons.push(geojsonFeature);
            mapLayer.addDataToRoutingLayer(geojsonFeature);
            var oneTab = $("<div class='route_result_tab'>");
            routeResultsDiv.append(oneTab);
            tabHeader.click(createClickHandler(geoJsons, pathIndex, tabHeader, oneTab, request.hasElevation(), request.useMiles));

            var routeInfo = $("<div class='route_description'>");
            if (path.description && path.description.length > 0) {
                routeInfo.text(path.description);
                routeInfo.append("<br/>");
            }

            var tempDistance = translate.createDistanceString(path.distance, request.useMiles);
            var tempRouteInfo;
            if(request.isPublicTransit()) {
                var tempArrTime = moment(ghRequest.getEarliestDepartureTime())
                                        .add(path.time, 'milliseconds')
                                        .format('LT');
                if(path.transfers >= 0)
                    tempRouteInfo = translate.tr("pt_route_info", [tempArrTime, path.transfers, tempDistance]);
                else
                    tempRouteInfo = translate.tr("pt_route_info_walking", [tempArrTime, tempDistance]);
            } else {
                var tmpDuration = translate.createTimeString(path.time);
                tempRouteInfo = translate.tr("route_info", [tempDistance, tmpDuration]);
            }

            routeInfo.append(tempRouteInfo);

            var kmButton = $("<button class='plain_text_button " + (request.useMiles ? "gray" : "") + "'>");
            kmButton.text(translate.tr2("km_abbr"));
            kmButton.click(createUnitsChooserButtonClickHandler(false));

            var miButton = $("<button class='plain_text_button " + (request.useMiles ? "" : "gray") + "'>");
            miButton.text(translate.tr2("mi_abbr"));
            miButton.click(createUnitsChooserButtonClickHandler(true));

            var buttons = $("<span style='float: right;'>");
            buttons.append(kmButton);
            buttons.append('|');
            buttons.append(miButton);

            routeInfo.append(buttons);

            if (request.hasElevation()) {
                routeInfo.append(translate.createEleInfoString(path.ascend, path.descend, request.useMiles));
            }

            routeInfo.append($("<div style='clear:both'/>"));
            oneTab.append(routeInfo);

            if (path.instructions) {
                var instructions = require('./instructions.js');
                oneTab.append(instructions.create(mapLayer, path, urlForHistory, request));
            }

            var detailObj = path.details;
            if(detailObj && request.api_params.debug) {
                // detailKey, would be for example average_speed
                for (var detailKey in detailObj) {
                    var pathDetailsArr = detailObj[detailKey];
                    for (i = 0; i < pathDetailsArr.length; i++) {
                        var pathDetailObj = pathDetailsArr[i];
                        var firstIndex = pathDetailObj[0];
                        var value = pathDetailObj[2];
                        var lngLat = path.points.coordinates[firstIndex];
                        L.marker([lngLat[1], lngLat[0]], {
                            icon: L.icon({
                                iconUrl: './img/marker-small-blue.png',
                                iconSize: [15, 15]
                            }),
                            draggable: true,
                            autoPan: true
                        }).addTo(mapLayer.getRoutingLayer()).bindPopup(detailKey + ":" + value);
                    }
                }
            }
        }
        // already select best path
        firstHeader.click();

        mapLayer.adjustMapSize();
        // TODO change bounding box on click
        var firstPath = json.paths[0];
        if (firstPath.bbox && doZoom) {
            var minLon = firstPath.bbox[0];
            var minLat = firstPath.bbox[1];
            var maxLon = firstPath.bbox[2];
            var maxLat = firstPath.bbox[3];
            var tmpB = new L.LatLngBounds(new L.LatLng(minLat, minLon), new L.LatLng(maxLat, maxLon));
            mapLayer.fitMapToBounds(tmpB);
        }

        $('.defaulting').each(function (index, element) {
            $(element).css("color", "black");
        });
    });
}

function mySubmit() {
    var fromStr,
            toStr,
            viaStr,
            allStr = [],
            inputOk = true;
    var location_points = $("#locationpoints > div.pointDiv > input.pointInput");
    var len = location_points.size;
    $.each(location_points, function (index) {
        if (index === 0) {
            fromStr = $(this).val();
            if (fromStr !== translate.tr("from_hint") && fromStr !== "")
                allStr.push(fromStr);
            else
                inputOk = false;
        } else if (index === (len - 1)) {
            toStr = $(this).val();
            if (toStr !== translate.tr("to_hint") && toStr !== "")
                allStr.push(toStr);
            else
                inputOk = false;
        } else {
            viaStr = $(this).val();
            if (viaStr !== translate.tr("via_hint") && viaStr !== "")
                allStr.push(viaStr);
            else
                inputOk = false;
        }
    });
    if (!inputOk) {
        // TODO print warning
        return;
    }
    if (fromStr === translate.tr("from_hint")) {
        // no special function
        return;
    }
    if (toStr === translate.tr("to_hint")) {
        // lookup area
        ghRequest.from.setStr(fromStr);
        $.when(resolveFrom()).done(function () {
            mapLayer.focus(ghRequest.from, null, 0);
        });
        return;
    }
    // route!
    if (inputOk)
        resolveCoords(allStr);
}

function isProduction() {
    return host.indexOf("graphhopper.com") > 0;
}

/**test add coordinates**/
/*
function testGPX(){
    var gpx_lat_input = document.getElementById('gpxlat');
    var gpx_lat = gpx_lat_input.value;
    var gpx_lon_input = document.getElementById('gpxlon');
    var gpx_lon = gpx_lon_input.value;

    var latlonArray = [];

    var GPXc = new GHCustom();

    GPXc.createGPXURL(gpx_lat,gpx_lon);

    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log("this is json");
        console.log(json);

        var GPX_Point =json.GPX_Point;

        latlonArray = GPX_Point[count].split(',');
        mapLayer.createMarkerGPX(latlonArray[1],latlonArray[0]);

        path_point.push(latlonArray);

        if(count == 0 || count == 1)
            path_snapped_waypoints.push(latlonArray);

        if(count > 1)
        {
            path_snapped_waypoints.pop();
            path_snapped_waypoints.push(latlonArray);
        }
        count++;

    });

    console.log(GPXc.GPXurl);

}*/

function drawLine(){

    var defaultRouteStyle = {color: "#006666", "weight": 5, "opacity": 0.6};
    var geojsonFeature = {
        "type": "Feature",
        "geometry": {
            "type":"LineString",
            "coordinates":path_point},
        "properties": {
            "style": defaultRouteStyle,
            name: "route",
            snapped_waypoints: path_snapped_waypoints
        }
    };

    if(path_point.length >= 2)
        mapLayer.addDataToRoutingLayer(geojsonFeature);
}

function drawLine3(){

    var defaultRouteStyle = {color: "#463CFF", "weight": 5, "opacity": 0.6};
    var geojsonFeature = {
        "type": "Feature",
        "geometry": {
            "type":"LineString",
            "coordinates":path_point},
        "properties": {
            "style": defaultRouteStyle,
            name: "route",
            snapped_waypoints: path_snapped_waypoints
        }
    };

    if(path_point.length >= 2)
        mapLayer.addDataToRoutingLayer(geojsonFeature);
}

function drawLine2(){

    var defaultRouteStyle = {color: "#FF8963", "weight": 5, "opacity": 0.6};
    var geojsonFeature = {
        "type": "Feature",
        "geometry": {
            "type":"LineString",
            "coordinates":path_point},
        "properties": {
            "style": defaultRouteStyle,
            name: "route",
            snapped_waypoints: path_snapped_waypoints
        }
    };

    if(path_point.length >= 2)
        mapLayer.addDataToRoutingLayer(geojsonFeature);
}


/**Clear Line and marker**/
function ClearLayer() {
    mapLayer.clearLayers();
}

/**navigation gps location to home**/
function RoutingLocation() {
    var GHloc = new GHLocation();
    GHloc.getLocation();
}

/**set time to go get the location**/
function Location(boolean) {

    /**set init 24 sec sampling rate 1 hz**/
    if(boolean === true && initCount === 0)
        window.IntervalLocation = setInterval(getLocation,1000);

    if(boolean === true && initCount === 25){
        clearInterval(window.IntervalLocation);
        window.IntervalLocation = setInterval(getLocation,5000);
    }

    if(boolean === false){
        clearInterval(window.IntervalLocation);
        initCount = 25;
    }

    console.log(IntervalLocation);
}

/**Get Location GPS Node**/
function getLocation() {
	var x = document.getElementById("gps_Location");
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(showPosition, showError);
    } else {
        x.innerHTML = "Geolocation is not supported by this browser.";
    }
}

/**callback function to success**/
function showPosition(position) {
	var x = document.getElementById("gps_Location");
    var latlonArray = [];

	//var timestamp = new Date(position.timestamp);
    var timestamp = new Date();

	var time = timestamp.getFullYear()+"-"+ (timestamp.getMonth()+1)+"-"+timestamp.getDate()+" "
              +timestamp.getHours()+":"+timestamp.getMinutes()+":"+timestamp.getSeconds();

    x.innerHTML ="gps:"+
        "<br>Latitude:" + position.coords.latitude +
        "<br>Longitude: " + position.coords.longitude +
        "<br>Accuracy: "+ position.coords.accuracy +
        "<br>Time:" + time ;

    var GPXc = new GHCustom();
    GPXc.createGPXNode(position.coords.latitude,position.coords.longitude,position.coords.accuracy,time)
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log(json);

        if(json.GPX_Point){
            var GPX_Point =json.GPX_Point;
            var GPX_length = GPX_Point.length;
            if(count < GPX_length){
                for(var i = count; i < GPX_length; i++){
                    latlonArray = GPX_Point[i].split(',');
                    mapLayer.createMarkerGPX(latlonArray[1], latlonArray[0]);
                    path_point.push(latlonArray);
                }
                count = GPX_length;
            }
        }
        /*
        if(json.BBox){
            var BBoxBounds = json.BBox;
            if(BBoxBounds.length > 0){
                var minLon = BBoxBounds[3];
                var minLat = BBoxBounds[2];
                var maxLon = BBoxBounds[1];
                var maxLat = BBoxBounds[0];
                var tmpB = new L.LatLngBounds(new L.LatLng(minLat, minLon), new L.LatLng(maxLat, maxLon));
                mapLayer.fitMapToBounds(tmpB);
            }
        }*/
    });

    drawLine();

    console.log(GPXc.GPXurl);

    if(initCount < 26)
        initCount++;
    if(initCount === 25)
        Location(true);

}

/**callback function to fail**/
function showError(error) {
	var x = document.getElementById("gps_Location");
	
    switch(error.code) {
        case error.PERMISSION_DENIED:
            x.innerHTML = "User denied the request for Geolocation.";
            break;
        case error.POSITION_UNAVAILABLE:
            x.innerHTML = "Location information is unavailable.";
            break;
        case error.TIMEOUT:
            x.innerHTML = "The request to get user location timed out.";
            break;
        case error.UNKNOWN_ERROR:
            x.innerHTML = "An unknown error occurred.";
            break;
    }
}

/**routing function**/
function routing() {
    var pathindex = 0;
    var GPXc = new GHCustom();
    GPXc.route(PositionCoord.lat,PositionCoord.lng,HomeCoordObject.lat,HomeCoordObject.lng);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log("this is json");
        console.log(json);

        var path = json.paths[pathindex];
        path_point = path.points.coordinates;
        path_snapped_waypoints = path.snapped_waypoints.coordinates;
        //mapLayer.createMarkerGPX(path_snapped_waypoints[0][1],path_snapped_waypoints[0][0]);
        drawLine();

        var firstPath = json.paths[pathindex];
        if (firstPath.bbox) {
            var minLon = firstPath.bbox[0];
            var minLat = firstPath.bbox[1];
            var maxLon = firstPath.bbox[2];
            var maxLat = firstPath.bbox[3];
            var tmpB = new L.LatLngBounds(new L.LatLng(minLat, minLon), new L.LatLng(maxLat, maxLon));
            mapLayer.fitMapToBounds(tmpB);
        }
    });
    console.log(GPXc.GPXurl);
}

/**delay function, not use**/
function sleep(sec){
    var time = new Date().getTime();
    while (new Date().getTime() - time < sec * 1000);
}

/**Map Matching**/
function MapMatching(){

    var GPXc = new GHCustom();
    GPXc.MapMatching(0,0);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log("this is json");
        console.log(json);
    });

    console.log(GPXc.GPXurl);
}

/**query storage gpx file**/
function displayFile() {

    var GPXc = new GHCustom();
    GPXc.GetGPXFile(0,0);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log(json);
        var file =json.GPXFile;

        if(file.length !== 0){
            $("#gpxFile").empty();
            for(var index in file){
                $("#gpxFile").append($("<option></option>").text(file[index]));
            }
        }
    });
}

/**select gpx filename for training**/
function selectFile(){

    var GPXc = new GHCustom();
    var str = null;
    var latlonArray = [];

    $("#gpxFile").find(":selected").each(function() {
        if(this.text !== 'null'){
            var strlength = this.text.length;
            if(strlength === 13)
                str = this.text.slice(8,9);
            else
                str = this.text.slice(8,10);
            GPXc.TrainFile(0,0,str);

            GPXc.doRequest(GPXc.GPXurl, function (json) {
                console.log(json);
                var GPX_Point = json.GPX_Point;
                for(var p = 0; p < GPX_Point.length; p++){
                    latlonArray = GPX_Point[p].split(',');
                    path_point.push(latlonArray);
                    if(p===0)
                        path_snapped_waypoints.push(latlonArray);
                    if(p===GPX_Point.length-1)
                        path_snapped_waypoints.push(latlonArray);
                }
                mapLayer.createMarkerGPX(path_snapped_waypoints[0][1],path_snapped_waypoints[0][0]);
                mapLayer.createMarkerGPX(path_snapped_waypoints[1][1],path_snapped_waypoints[1][0]);
                drawLine3();
                path_point = [];
                path_snapped_waypoints =[];
            });
        }else{
            alert("please choose gpx file!")
        }
    });

}

/**Display Raw Trajectory**/
function DisplayFileRawTrajectory() {
    var GPXc = new GHCustom();
    var str = null;
    var latlonArray = [];

    $("#gpxFile").find(":selected").each(function() {
        if(this.text !== 'null'){
            var strlength = this.text.length;
            if(strlength === 13)
                str = this.text.slice(8,9);
            else
                str = this.text.slice(8,10);
            GPXc.DisplayFileTrajectory(0,0,str);

            GPXc.doRequest(GPXc.GPXurl, function (json) {
                console.log(json);
                var GPX_Point = json.GPX_Point;
                for(var p = 0; p < GPX_Point.length; p++){
                    latlonArray = GPX_Point[p].split(',');
                    mapLayer.createMarkerGPX(latlonArray[1], latlonArray[0]);
                    path_point.push(latlonArray);
                }
                drawLine2();
                path_point = [];
                path_snapped_waypoints =[];
            });
        }else{
            alert("please choose gpx file!")
        }
    });
}

/**easy instruction of Storage Stay Place data**/
function StorageStayPlace(){

    var GPXc = new GHCustom();
    GPXc.StorageStayPlace(0,0);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log(json);
    });
}

/**Export GPX File instruction**/
function ExportGPXFile(){

    var GPXc = new GHCustom();
    GPXc.ExportGPXFile(0,0);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log(json);
    });
}

/**Display Stay Point**/
function DisplayStayPoint(){

    var latlonArray = [];
    var GPXc = new GHCustom();
    GPXc.DisplayStay(0,0);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log(json);
        var GPX_Point = json.GPX_Point;
        var Stay_length = GPX_Point.length;

        if(Stay_length != null){
            for(var p = 0; p < Stay_length; p++){
                latlonArray = GPX_Point[p].split(',');
                mapLayer.createMarkerStay(latlonArray[1], latlonArray[0]);
            }

            if(json.BBox){
                var BBoxBounds = json.BBox;
                if(BBoxBounds.length > 0){
                    var minLon = BBoxBounds[3];
                    var minLat = BBoxBounds[2];
                    var maxLon = BBoxBounds[1];
                    var maxLat = BBoxBounds[0];
                    var tmpB = new L.LatLngBounds(new L.LatLng(minLat, minLon), new L.LatLng(maxLat, maxLon));
                    mapLayer.fitMapToBounds(tmpB);
                }
            }
        }
    });
    console.log(GPXc.GPXurl);
}

function onlyDisplayTrajectoryLine() {
    mapLayer.clearLayers();
    drawLine();
}

/**Display GPX Trajectory**/
function DisplayGPXTrajectory(){

    var latlonArray = [];
    var GPXc = new GHCustom();
    GPXc.DisplayTrajectory(0,0);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log(json);
        var GPX_Point = json.GPX_Point;

        if(GPX_Point.length > 0){
            for(var p = 0; p < GPX_Point.length; p++){
                latlonArray = GPX_Point[p].split(',');
                path_point.push(latlonArray);
                mapLayer.createMarkerGPX(latlonArray[1], latlonArray[0]);
            }
        }

        drawLine();

        if(json.BBox){
            var BBoxBounds = json.BBox;
            if(BBoxBounds.length > 0){
                var minLon = BBoxBounds[3];
                var minLat = BBoxBounds[2];
                var maxLon = BBoxBounds[1];
                var maxLat = BBoxBounds[0];
                var tmpB = new L.LatLngBounds(new L.LatLng(minLat, minLon), new L.LatLng(maxLat, maxLon));
                mapLayer.fitMapToBounds(tmpB);
            }
        }
    });

}

/**Experiment 1**/
function ExperimentTrajectory(){

    var latlonArray = [];
    var GPXc = new GHCustom();
    GPXc.ExperimentTrajectory(0,0);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log(json);

        var GPX_Point = json.GPX_Point;

        if(GPX_Point.length > 0){
            for(var p = 0; p < GPX_Point.length; p++){
                latlonArray = GPX_Point[p].split(',');
                path_point.push(latlonArray);
                mapLayer.createMarkerGPX(latlonArray[1], latlonArray[0]);
            }
        }

        drawLine2();
    });
}

/**navigation**/
module.exports.routing = function(routingFromLat,routingFromLon){

    var pathindex = 0;
    var GPXc = new GHCustom();
    GPXc.route(routingFromLat,routingFromLon,HomeCoordObject.lat,HomeCoordObject.lng);
    GPXc.doRequest(GPXc.GPXurl, function (json) {
        console.log("this is json");
        console.log(json);

        var path = json.paths[pathindex];
        path_point = path.points.coordinates;
        path_snapped_waypoints = path.snapped_waypoints.coordinates;
        mapLayer.createMarkerGPX(path_snapped_waypoints[0][1],path_snapped_waypoints[0][0]);
        drawLine();

        var firstPath = json.paths[pathindex];
        if (firstPath.bbox) {
            var minLon = firstPath.bbox[0];
            var minLat = firstPath.bbox[1];
            var maxLon = firstPath.bbox[2];
            var maxLat = firstPath.bbox[3];
            var tmpB = new L.LatLngBounds(new L.LatLng(minLat, minLon), new L.LatLng(maxLat, maxLon));
            mapLayer.fitMapToBounds(tmpB);
        }
    });
    console.log(GPXc.GPXurl);
};

module.exports.setFlag = setFlag;
