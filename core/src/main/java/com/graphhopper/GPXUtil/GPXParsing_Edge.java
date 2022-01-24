package com.graphhopper.GPXUtil;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.EdgeIteratorState;

import java.util.ArrayList;
import java.util.List;

/**not use**/
public class GPXParsing_Edge {

    private ArrayList<Integer> edge_ID = new ArrayList<>();

    /**use method 4:1 get closest node and edge**/
    public PointListCustom ParseMatchingEdge (LocationIndex locationIndex,PointListCustom plc_input){
        PointListCustom plc_output = new PointListCustom();

        for(int i =0; i<plc_input.size()-1; i++){
            double lat_temp51 = (plc_input.getLat(i) * 4 + plc_input.getLat(i+1)) / 5;
            double lon_temp51 = (plc_input.getLon(i) * 4 + plc_input.getLon(i+1)) / 5;

            QueryResult qr = locationIndex.findClosest(lat_temp51, lon_temp51, EdgeFilter.ALL_EDGES);
            EdgeIteratorState qrClosestEdge = qr.getClosestEdge();

            edge_ID.add(qrClosestEdge.getEdge());
            plc_output.add(lat_temp51,lon_temp51);
        }
        return plc_output;
    }

    public void getEdgeID(){
        System.out.println("get edge ID: "+edge_ID);
    }



}
