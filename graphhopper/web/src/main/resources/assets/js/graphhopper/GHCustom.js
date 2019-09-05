
var graphhopperTools = require('./tools.js');

var GHCustom = function () {
  this.GPXurl = "";
  this.dataType = "json";
  this.do_request = false;
  this.elevation = false;
};

/**test only**/
GHCustom.prototype.createGPXURL = function(lat,lon){
   this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon ;
};

GHCustom.prototype.route = function(fromlat,fromlon,tolat,tolon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + fromlat + "%2C" +fromlon + "&point=" + tolat + "%2C" + tolon + "&routing=t";
}

/**real use**/
GHCustom.prototype.createGPXNode = function(lat,lon,acc,time){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&accuracy=" + acc +"&time=" +time ;
};

GHCustom.prototype.SetHomeNode = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&setHome=t";
}

GHCustom.prototype.hasElevation = function () {
    return this.elevation;
};

GHCustom.prototype.MapMatching = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&MapMatching=t";
}

GHCustom.prototype.GetGPXFile = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&file=t";
}

GHCustom.prototype.ExportGPXFile = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&export=t";
}

GHCustom.prototype.StorageStayPlace = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&stay=t";
}

GHCustom.prototype.TrainFile = function(lat,lon,index){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&train=t&index="+index;
}

GHCustom.prototype.DisplayStay = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&display=t";
}

GHCustom.prototype.DisplayTrajectory = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&trajectory=t";
}

GHCustom.prototype.DisplayFileTrajectory = function(lat,lon,index){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&rawTrajectory=t&index="+index;
}

GHCustom.prototype.ExperimentTrajectory = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&experiment=t";
}

GHCustom.prototype.SetStayPlace = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&setStay=t";
}

GHCustom.prototype.SetNowPosition = function(lat,lon){
    this.GPXurl = "https://pmcn-graphhopper.tk/gpx?point=" + lat + "%2C" +lon +"&setPosition=t";
}

GHCustom.prototype.doRequest = function (url ,callback) {
    var that = this;
    $.ajax({
        timeout: 30000,
        url: url,
        success: function (json) {
            //alert('Ajax request success!');
            if(json.encoded){
                console.log("Receive!");

                for (var i = 0; i < json.paths.length; i++) {
                    var path = json.paths[i];

                    // convert encoded polyline to geo json
                    var tmpArray = graphhopperTools.decodePath(path.points, that.elevation);
                    path.points = {
                        "type": "LineString",
                        "coordinates": tmpArray
                    };

                    var tmpSnappedArray = graphhopperTools.decodePath(path.snapped_waypoints, that.hasElevation());
                    path.snapped_waypoints = {
                        "type": "MultiPoint",
                        "coordinates": tmpSnappedArray
                    };
                }

            }
            callback(json);
        },
        error: function () {
            alert('Ajax request error');
        },
        type: "GET",
        dataType: this.dataType,
        crossDomain: true
        //dataType: "text"
    });
};

module.exports = GHCustom;
