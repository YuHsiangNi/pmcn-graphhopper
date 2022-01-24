
/**get routing current gps node*/

var mainTemplate = require('../main-template.js');

var GHLocation = function () {
    this.location_lat = null;
    this.location_lon = null;
};

function showPosition(position) {

    var x = document.getElementById("gps_Location");
    var timestamp = new Date(position.timestamp);

    var time = timestamp.getFullYear()+"-"+ (timestamp.getMonth()+1)+"-"+timestamp.getDate()+" "
        +timestamp.getHours()+":"+timestamp.getMinutes()+":"+timestamp.getSeconds();

    x.innerHTML = "Latitude:" + position.coords.latitude +
        "<br>Longitude: " + position.coords.longitude +
        "<br>Accuracy: "+ position.coords.accuracy +
        "<br>Time:" + time ;

    mainTemplate.routing(position.coords.latitude,position.coords.longitude);
}

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


GHLocation.prototype.getLocation = function () {

    var x = document.getElementById("gps_Location");

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(showPosition, showError);
    } else {
        x.innerHTML = "Geolocation is not supported by this browser.";
    }
};

GHLocation.prototype.setLat = function(lat){
    this.location_lat =lat;
};

GHLocation.prototype.setLon = function(lon){
    this.location_lon =lon;
};

GHLocation.prototype.getLat = function(){
    return this.location_lat;
};

GHLocation.prototype.getLon = function(){
    return this.location_lon;
};

GHLocation.prototype.lookup = function(){
  console.log(this.location_lat+','+this.location_lon);
};

module.exports = GHLocation;


