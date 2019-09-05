package com.graphhopper.MapMatching;

import com.graphhopper.Database.DBHelper;
import com.graphhopper.GPXUtil.GPXWriter;
import com.graphhopper.GPXUtil.PointListCustom;
import com.graphhopper.GraphHopper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXFile;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.shapes.GHPoint;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 author Yu-Hsiang Lin
 **/


public class GPXMapMatching {

    private List<GPXEntry> Entries = new ArrayList<>();
    private ArrayList<Integer> edge_ID = new ArrayList<>();
    private ArrayList<String>  MatchingPoint_Array = new ArrayList<String>();
    private GraphHopper graphHopper;
    public ArrayList<String> EdgeName = new ArrayList<>();

    public GPXMapMatching(GraphHopper graphHopper){
        this.graphHopper = graphHopper;
    }

    public void WithMapMatching(){

        AlgorithmOptions options = AlgorithmOptions.start()
                .algorithm("dijkstrabi")
                .weighting(graphHopper.getWeighting())
                .build();

        MapMatching matching = new MapMatching(graphHopper,options);
        MatchResult matchResult = matching.doWork(Entries);
        GPXMatchResult(matchResult);
    }

    private void GPXMatchResult(MatchResult mr){
        List<GPXEntry> resultEntries = new ArrayList<GPXEntry>(mr.getEdgeMatches().size());

        long time = 0;
        double distance = 0;
        for (int emIndex = 0; emIndex < mr.getEdgeMatches().size(); emIndex++) {
            EdgeMatch em = mr.getEdgeMatches().get(emIndex);
            PointList pl = em.getEdgeState().fetchWayGeometry(emIndex == 0 ? 3 : 2);
            edge_ID.add(em.getEdgeState().getEdge());
            distance += em.getEdgeState().getDistance();
            System.out.println("Edge: " + em.getEdgeState().getEdge() + " Length:" + em.getEdgeState().getDistance() + " Name:" + em.getEdgeState().getName());
            EdgeName.add(em.getEdgeState().getName());

            for (int i = 0; i < pl.size(); i++) {
                if (pl.is3D()) {
                    resultEntries.add(new GPXEntry(pl.getLatitude(i), pl.getLongitude(i), pl.getElevation(i), time));
                    MatchingPoint_Array.add(pl.getLon(i) +","+pl.getLat(i));
                } else {
                    resultEntries.add(new GPXEntry(pl.getLatitude(i), pl.getLongitude(i), time));
                    MatchingPoint_Array.add(pl.getLon(i) +","+pl.getLat(i));
                }
            }
        }

        System.out.println("MapMatching get Edge:" + edge_ID + " Total Distance:" + distance);
        //consider next times training data
        Entries.clear();

        /**Write to Map Matching Edge of training**/
        TrainGpxForDB();
    }


    public void getEntries(PointListCustom pointListCustom){

        long time = 0;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for(int s = 0; s < pointListCustom.size(); s++){

            try {
                time = simpleDateFormat.parse(pointListCustom.getTime(s)).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Entries.add(new GPXEntry(pointListCustom.getLat(s),pointListCustom.getLon(s), time));
        }
    }

    /*test GPX data*/
    public void TestPLC(){

        PointListCustom TestpointList = new PointListCustom();

        TestpointList.add(22.9766397,120.217477,Double.NaN,10.0,"2019-3-26 15:10:11");
        TestpointList.add(22.9766527,120.2174381,Double.NaN,10.0,"2019-3-26 15:10:26");
        TestpointList.add(22.9756263,120.218646,Double.NaN,10.0,"2019-3-26 15:10:41");
        TestpointList.add(22.974982,120.2180133,Double.NaN,10.0,"2019-3-26 15:10:56");
        TestpointList.add(22.9747906,120.2174254,Double.NaN,10.0,"2019-3-26 15:11:11");
        TestpointList.add(22.9747324,120.2174677,Double.NaN,10.0,"2019-3-26 15:11:26");
        TestpointList.add(22.9746623,120.2173099,Double.NaN,10.0,"2019-3-26 15:11:41");
        TestpointList.add(22.9748493,120.217469,Double.NaN,10.0,"2019-3-26 15:11:56");
        TestpointList.add(22.9745452,120.2173843,Double.NaN,10.0,"2019-3-26 15:12:11");
        TestpointList.add(22.975622,120.2163928,Double.NaN,10.0,"2019-3-26 15:12:26");
        TestpointList.add(22.9772138,120.2154692,Double.NaN,10.0,"2019-3-26 15:12:41");
        TestpointList.add(22.9777258,120.2153269,Double.NaN,10.0,"2019-3-26 15:12:56");
        TestpointList.add(22.9779071,120.2149324,Double.NaN,10.0,"2019-3-26 15:13:11");
        TestpointList.add(22.977882,120.2143303,Double.NaN,10.0,"2019-3-26 15:13:26");
        TestpointList.add(22.9778372,120.213682,Double.NaN,10.0,"2019-3-26 15:13:41");

        getEntries(TestpointList);
        WithMapMatching();
    }

    /**Training GPX Edge**/
    private void TrainGpxForDB(){
        //int dateCount = 9;
        DBHelper dbHelper = new DBHelper();

        /*test GET DB Data*/
        try {
            dbHelper.DBConnection();
            //dateCount = dateCount + dbHelper.getTrainingTimes();
            dbHelper.TrainEdgeWeighting(edge_ID,getTime());

            dbHelper.DBClose();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private String getTime(){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date newTime = new Date();

        return simpleDateFormat.format(newTime);
    }

    public ArrayList<String> getStreetName(){
        return EdgeName;
    }


    /**consider to return web GPX Point Array**/
    public ArrayList<String> GPXdoImport(int version){

        String fileName = "TrackGPX" + version +".gpx";

        GPXFile gpxFile = new GPXFile();
        Entries = gpxFile.doImport("trackgpx/"+ fileName).getEntries();
        WithMapMatching();

        return MatchingPoint_Array;
    }

    /**experiment use every day train**/
    private String getExperimentDate(int dateCount){
        return "2019-06-" + dateCount + " 00:00:00";
    }


}
