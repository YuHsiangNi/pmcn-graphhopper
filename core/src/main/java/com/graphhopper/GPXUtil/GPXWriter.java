package com.graphhopper.GPXUtil;

import com.graphhopper.util.Helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

/**
 author Yu-Hsiang Lin
 **/


public class GPXWriter {

    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    static final String DATE_FORM ="yyyy-MM-dd HH:mm:ss";

    public boolean createFile(PointListCustom plcTrack, int version){

        //create directory
        if(!createDirectory())
            return false;

        String fileName = "TrackGPX" + version +".gpx";
        File gpxFile = new File("trackgpx/"+ fileName);

        if(!gpxFile.exists()){
            try {
                gpxFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("not find directory");
        }

        if(gpxFile.exists()){
            doExport(writeGPX(plcTrack,version),gpxFile);
            return true;
        }
        else
            return false;
    }

    private boolean createDirectory(){
        File fileDir = new File("trackgpx");
        //System.out.println(fileDir.getAbsolutePath());
        if(!fileDir.isDirectory())
            return fileDir.mkdirs();
        return true;
    }

    private String writeGPX(PointListCustom plcTrack,int version){
        long startTimeMillis = 0;
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORM);

        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>"
                + "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " creator=\"Graphhopper version " + version + "\" version=\"1.1\""
                // This xmlns:gh acts only as ID, no valid URL necessary.
                // Use a separate namespace for custom extensions to make basecamp happy.
                + " xmlns:gh=\"https://graphhopper.com/public/schema/gpx/1.1\">"
                + "\n<metadata>"
                + "<copyright author=\"OpenStreetMap contributors\"/>"
                + "<link href=\"http://graphhopper.com\">"
                + "<text>GraphHopper GPX</text>"
                + "</link>"
                + "<time>" + formatter.format(startTimeMillis) + "</time>"
                + "</metadata>";

        StringBuilder gpxOutput = new StringBuilder(header);
        gpxOutput.append("\n<trk><name>").append("GraphHopper GPX Track").append("</name>");

        gpxOutput.append("<trkseg>");
        for(int num=0; num < plcTrack.size(); num++){
            gpxOutput.append("\n<trkpt lat='").append(Helper.round6(plcTrack.getLat(num)));
            gpxOutput.append("' lon='").append(Helper.round6(plcTrack.getLon(num))).append("'>");

            try {
                Date date = simpleDateFormat.parse(plcTrack.getTime(num));
                String CertDate = formatter.format(date);
                gpxOutput.append("<time>").append(CertDate).append("</time>");
            } catch (ParseException e) {
                e.printStackTrace();
            }

            gpxOutput.append("</trkpt>");
        }

        gpxOutput.append("\n</trkseg>");
        gpxOutput.append("\n</trk>");

        // we could now use 'wpt' for via points
        gpxOutput.append("\n</gpx>");

        return gpxOutput.toString().replaceAll("\\'", "\"");
    }

    private void doExport(String gpxOutput,File gpxFile){
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(gpxFile));
            writer.append(gpxOutput);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            Helper.close(writer);
        }
    }

    /**display track folder  all file**/
    public ArrayList<String> displayFile(){
        ArrayList<String> FolderFile = new ArrayList<String>();

        try{
            File gpxFile = new File("trackgpx/");
            String[] list = gpxFile.list();

            assert list != null;
            Collections.addAll(FolderFile, list);

            System.out.println(FolderFile);
        }catch(Exception e){
            System.out.println("folder is not exits");
        }

        return FolderFile;
    }

    /**determine file version**/
    public int VersionFile(){
        File gpxFile = new File("trackgpx/");
        String[] list = gpxFile.list();

        return list.length;
    }

}
