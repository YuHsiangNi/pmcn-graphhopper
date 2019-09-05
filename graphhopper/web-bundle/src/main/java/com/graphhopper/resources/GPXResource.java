package com.graphhopper.resources;


import com.graphhopper.*;
import com.graphhopper.Database.DBHelper;
import com.graphhopper.GPXUtil.GPXFilter;
import com.graphhopper.GPXUtil.GPXWriter;
import com.graphhopper.MapMatching.GPXMapMatching;
import com.graphhopper.http.WebHopper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 author Yu-Hsiang Lin
  **/

@Path("gpx")

public class GPXResource {

    private GHResponse ghResponse;
    private GraphHopper graphHopper;

    private ArrayList<String> GPX_Point_Array;
    private double fromlat,fromlon,tolat,tolon;

    @Inject
    public GPXResource (GraphHopper graphHopper, @Named("hasElevation") Boolean hasElevation) {
        this.graphHopper = graphHopper;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_PLAIN})
    public Response doGet(
            @QueryParam("point") List<GHPoint> gpxPoints,
            @QueryParam("setHome") @DefaultValue("f") String strhome,
            @QueryParam("accuracy")  @DefaultValue("0.0")  String acc,
            @QueryParam("time")  @DefaultValue(" ")  String time,
            @QueryParam("MapMatching") @DefaultValue("f") String mapMatching,
            @QueryParam("file") @DefaultValue("f") String file,
            @QueryParam("train") @DefaultValue("f") String train,
            @QueryParam("index") @DefaultValue("f") String index,
            @QueryParam("stay") @DefaultValue("f") String stay,
            @QueryParam("export") @DefaultValue("f") String export,
            @QueryParam("display") @DefaultValue("f") String display,
            @QueryParam("trajectory") @DefaultValue("f") String trajectory,
            @QueryParam("rawTrajectory") @DefaultValue("f") String rawFileTrajectory,
            @QueryParam("experiment") @DefaultValue("f") String experiment,
            @QueryParam("setStay") @DefaultValue("f") String setStay,
            @QueryParam("setPosition") @DefaultValue("f") String setPosition,
            @QueryParam("routing") @DefaultValue("f") String route){

        //route function
        if (route.equalsIgnoreCase("t"))
        {
            GHPoint fromPoint = gpxPoints.get(0);
            fromlat = fromPoint.getLat();
            fromlon = fromPoint.getLon();

            GHPoint toPoint = gpxPoints.get(1);
            tolat = toPoint.getLat();
            tolon = toPoint.getLon();

            System.out.println("from coordinate: "+ fromlat +','+ fromlon);
            System.out.println("to coordinate: "+ tolat +','+ tolon);

            ghResponse = graphHopper.calcPath(fromlat, fromlon,tolat, tolon);

            return Response.ok(WebHopper.RouteJsonObject(ghResponse)).build();

        //set home coordinate function
        } else if(strhome.equalsIgnoreCase("t")){

            GHPoint HomePoint = gpxPoints.get(0);
            tolat = HomePoint.getLat();
            tolon = HomePoint.getLon();

            return Response.ok(WebHopper.GResponse()).build();

        // real time Map Matching function
        } else if(mapMatching.equalsIgnoreCase("t")) {

            graphHopper.RealTimeMapMatching(graphHopper);

            return Response.ok(WebHopper.GResponse()).build();

        // query all file
        } else if(file.equalsIgnoreCase("t")) {

            GPXWriter  gpxWriter = new GPXWriter();

            return Response.ok(WebHopper.GPXFile(gpxWriter.displayFile())).build();

        // training gpx file
        } else if(train.equalsIgnoreCase("t")) {

            int GPXIndex = Integer.valueOf(index);
            GPXMapMatching gpxFileMapMatching = new GPXMapMatching(graphHopper);

            return Response.ok(WebHopper.JsonObjectV3(gpxFileMapMatching.GPXdoImport(GPXIndex),gpxFileMapMatching.EdgeName)).build();

         //Storage Stay Place
        } else if(stay.equalsIgnoreCase("t")) {

           graphHopper.StorageStayPoint();

            return Response.ok(WebHopper.GResponse()).build();
        //Export gpx file
        } else if(export.equalsIgnoreCase("t")) {

            graphHopper.ExportGPX();

            return Response.ok(WebHopper.GResponse()).build();

        //Display Stay Point
        } else if(display.equalsIgnoreCase("t")) {

            if(graphHopper.CheckStayPointSize())
                return Response.ok(WebHopper.JsonObjectV2(graphHopper.DisplayStayPoint(),graphHopper.getDisBBoxBounds("stay"))).build();
            else
                return Response.ok(WebHopper.GResponse()).build();

        //Display GPX Trajectory
        } else if(trajectory.equalsIgnoreCase("t")) {

            if(graphHopper.CheckTrajectorySize())
                return Response.ok(WebHopper.JsonObjectV2(graphHopper.DisplayTrajectory(),graphHopper.getDisBBoxBounds("trajectory"))).build();
            else
                return Response.ok(WebHopper.GResponse()).build();

        } else if(rawFileTrajectory.equalsIgnoreCase("t")) {

            int GPXIndex = Integer.valueOf(index);

            return Response.ok(WebHopper.JsonObject(graphHopper.gpxTrajectory(GPXIndex))).build();

        //experiment point filter algorithm
        } else if(experiment.equalsIgnoreCase("t")) {

            return Response.ok(WebHopper.JsonObject(graphHopper.ExperimentTrajectory())).build();

        //Set and Storage Stay Position
        } else if(setStay.equalsIgnoreCase("t")){

            GHPoint StayPoint = gpxPoints.get(0);
            double Staylat = StayPoint.getLat();
            double Staylon = StayPoint.getLon();

            graphHopper.StorageStayPoint(Staylat, Staylon);

            return Response.ok(WebHopper.GResponse()).build();

         //Set Now Position
        } else if(setPosition.equalsIgnoreCase("t")){

            GHPoint PositionPoint = gpxPoints.get(0);
            fromlat = PositionPoint.getLat();
            fromlon = PositionPoint.getLon();

            return Response.ok(WebHopper.GResponse()).build();
        }
        else {

            GHPoint GpxPoint = gpxPoints.get(0);
            GPX_Point_Array = graphHopper.GPS_Point_record(GpxPoint,acc,time);

            //if(graphHopper.CheckIfBBoxChange())
            //    return Response.ok(WebHopper.JsonObjectV2(GPX_Point_Array,graphHopper.getBBoxBounds())).build();
            //else
            return Response.ok(WebHopper.JsonObject(GPX_Point_Array)).build();
        }
    }

    //v2. @QueryParam + List<GHPoint> #List can point > 2
    /*@GET
    public Response doGet(
            @QueryParam("point") List<GHPoint> gpxPoints){

        String message = "Hello GPX";

        for(int i=0; i<gpxPoints.size(); i++){
            GHPoint point = gpxPoints.get(i);
            System.out.println(point);
        }
        return Response.status(200).entity(message).build();
    }*/

    // v1. Path + @PathParam
    /*@GET
    @Path("/{point}")
    public Response doGet(
        @PathParam("point") String gpxPoint){

        String message = "GPX = "+ gpxPoint;
        System.out.println(message);
        return Response.status(200).entity(message).build();
    }*/

}
