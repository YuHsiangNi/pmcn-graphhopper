package com.graphhopper.http;


import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

import java.util.ArrayList;

/**
 * Code which handles polyline encoding and other web stuff.
 *
 author Yu-Hsiang Lin
 **/


public class WebHopper {

    private static Boolean pointsEncoded = true;

    private static String encodePolyline(PointList poly, boolean includeElevation) {
        return encodePolyline(poly, includeElevation, 1e5);
    }

    private static String encodePolyline(PointList poly, boolean includeElevation, double precision) {
        StringBuilder sb = new StringBuilder();
        int size = poly.getSize();
        int prevLat = 0;
        int prevLon = 0;
        int prevEle = 0;
        for (int i = 0; i < size; i++) {
            int num = (int) Math.floor(poly.getLatitude(i) * precision);
            encodeNumber(sb, num - prevLat);
            prevLat = num;
            num = (int) Math.floor(poly.getLongitude(i) * precision);
            encodeNumber(sb, num - prevLon);
            prevLon = num;
            if (includeElevation) {
                num = (int) Math.floor(poly.getElevation(i) * 100);
                encodeNumber(sb, num - prevEle);
                prevEle = num;
            }
        }
        return sb.toString();
    }

    private static void encodeNumber(StringBuilder sb, int num) {
        num = num << 1;
        if (num < 0) {
            num = ~num;
        }
        while (num >= 0x20) {
            int nextValue = (0x20 | (num & 0x1f)) + 63;
            sb.append((char) (nextValue));
            num >>= 5;
        }
        num += 63;
        sb.append((char) (num));
    }

    public static ObjectNode JsonObject(ArrayList<String> pointList){

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.putPOJO("GPX_Point",pointList);

        return json;
    }

    public static ObjectNode JsonObjectV2(ArrayList<String> pointList, ArrayList<Double> BBoxList){

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.putPOJO("GPX_Point",pointList);
        json.putPOJO("BBox",BBoxList);

        return json;
    }

    public static ObjectNode JsonObjectV3(ArrayList<String> pointList, ArrayList<String> NameList){

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.putPOJO("GPX_Point",pointList);
        json.putPOJO("Street",NameList);

        return json;
    }


    public static ObjectNode RouteJsonObject(GHResponse ghRsp){

        boolean enableElevation = false;

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("encoded", pointsEncoded);

        ArrayNode jsonPathList = json.putArray("paths");

        for (PathWrapper ar : ghRsp.getAll()) {
            ObjectNode jsonPath = jsonPathList.addObject();
            jsonPath.put("distance", Helper.round(ar.getDistance(), 3));
            jsonPath.put("weight", Helper.round6(ar.getRouteWeight()));
            jsonPath.put("time", ar.getTime());
            jsonPath.putPOJO("points", pointsEncoded ? encodePolyline(ar.getPoints(), enableElevation) : ar.getPoints().toLineString(enableElevation));
            jsonPath.putPOJO("snapped_waypoints", pointsEncoded ? encodePolyline(ar.getWaypoints(), enableElevation) : ar.getWaypoints().toLineString(enableElevation));
            if (ar.getPoints().getSize() >= 2) {
                jsonPath.putPOJO("bbox", ar.calcBBox2D());
            }
            jsonPath.putPOJO("instructions", ar.getInstructions());
        }

        return json;
    }

    public static ObjectNode GResponse(){

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("message","success");

        return json;
    }

    public static ObjectNode GPXFile(ArrayList<String> allfile){

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.putPOJO("GPXFile",allfile);

        return json;
    }
}
