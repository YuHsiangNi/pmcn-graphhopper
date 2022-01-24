package com.graphhopper.GPXUtil;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.DistanceCalcEarth;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 author Yu-Hsiang Lin

 GPS Point Filter by Speed and Accuracy
 **/


public class GPXFilter {
    private DistanceCalcEarth distanceCalcEarth = new DistanceCalcEarth();

    private long fromPointTime =0;
    private long toPointTime =0;
    private int DynamicWindowSize =12;
    private int SameFilterCount = 0;

    public boolean FilterSpeedWithAcc(PointListCustom plc_input,PointListCustom plc_correct,int index){

        double limitMaxSpeed = 100;
        double SpeedToAverage = 0;
        double NowSpeed = getSpeed(plc_input,plc_correct,plc_correct.size()-1,index);

        if(plc_input.size() > DynamicWindowSize){
            SpeedToAverage = SpeedAverage(plc_input,index);

            return NowSpeed < limitMaxSpeed &&  (SpeedToAverage * 3) > NowSpeed && AccuracyWithFilter(plc_input,index) && TooClosePoint(plc_input,plc_correct,index);
        }
        else
            return NowSpeed < limitMaxSpeed && AccuracyWithFilter(plc_input,index) && TooClosePoint(plc_input,plc_correct,index);

    }

    private double getSpeed(PointListCustom plc_input, PointListCustom plc_correct ,int fromNode, int toNode){
        double Speed,fromLat,fromLon,toLat,toLon;
        double distance;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            fromPointTime = simpleDateFormat.parse(plc_correct.getTime(fromNode)).getTime();
            toPointTime = simpleDateFormat.parse(plc_input.getTime(toNode)).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        fromLat = plc_correct.getLat(fromNode);
        fromLon = plc_correct.getLon(fromNode);
        toLat = plc_input.getLat(toNode);
        toLon = plc_input.getLon(toNode);

        distance = distanceCalcEarth.calcDist(fromLat,fromLon,toLat,toLon);

        Speed = distance / (Math.abs(Double.valueOf(fromPointTime-toPointTime))/1000)*3.6;
        /*
        if(Speed > 0){
            System.out.println("index:" +fromNode);
            System.out.println("From: " + fromLat + ',' +fromLon);
            System.out.println("To: " + toLat + ',' +toLon);
            System.out.println("Speed: " + Speed);
            System.out.println("distance:"+ distance);
        }*/

        return Speed;
    }

    private double getSpeed(PointListCustom plc_input, int index){
        double Speed,fromLat,fromLon,toLat,toLon;
        double distance;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            fromPointTime = simpleDateFormat.parse(plc_input.getTime(index)).getTime();
            toPointTime = simpleDateFormat.parse(plc_input.getTime(index+1)).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        fromLat = plc_input.getLat(index);
        fromLon = plc_input.getLon(index);
        toLat = plc_input.getLat(index+1);
        toLon = plc_input.getLon(index+1);

        distance = distanceCalcEarth.calcDist(fromLat,fromLon,toLat,toLon);

        Speed = distance / (Math.abs(Double.valueOf(fromPointTime-toPointTime))/1000)*3.6;

        return Speed;
    }

    /**calc window Speed Average**/
    private double SpeedAverage(PointListCustom plc_input, int index){

        double[] Speed = new double[20];
        double sum = 0, SpeedAver = 0;
        int conut = 0;

        for(int s = index > DynamicWindowSize ? index - DynamicWindowSize : 0; index + DynamicWindowSize < plc_input.size() -1 ? s < index + DynamicWindowSize : s < plc_input.size()-1; s++){
            Speed[conut] = getSpeed(plc_input,s);
            conut++;
        }

        for(int j = 0 ; j < DynamicWindowSize ; j++){
            sum += Speed[j];
        }

        SpeedAver = sum / (DynamicWindowSize);

        System.out.println("Speed Average: " + SpeedAver);

        return SpeedAver;

    }

    /**experiment to calc total Speed Average**/
    public void SpeedAverage(PointListCustom plc_input){

        double sum = 0, SpeedAver = 0;

        for(int s = 0; s < plc_input.size()-1; s++){
            sum = sum + getSpeed(plc_input,s);
        }

        SpeedAver = sum / (plc_input.size()-1);

        System.out.println("Speed Average: " + SpeedAver);

    }

    /**experiment to calc total Accuracy Average**/
    public double AccuracyAverage(PointListCustom plc_input){

        double sum = 0, AccAver = 0;

        for(int s = 0; s < plc_input.size(); s++){
            sum = sum + plc_input.getAccuracy(s);
        }

        AccAver = sum / plc_input.size();

        return AccAver;
    }

    /**experiment to calc total Accuracy Standard Deviation **/
    public void AccuracySD(PointListCustom plc_input){
        double variacne = 0 , StandardDeviationAccuracy = 0 , averageAccuracy = 0;

        averageAccuracy = AccuracyAverage(plc_input);

        for(int a = 0; a < plc_input.size(); a++){
            variacne += Math.pow(plc_input.getAccuracy(a) - averageAccuracy, 2);
        }

        StandardDeviationAccuracy = Math.sqrt(variacne / plc_input.size());

        System.out.println("Accuracy Average: " + averageAccuracy);
        System.out.println("Accuracy Standard Deviation: " + StandardDeviationAccuracy);
    }

    private boolean AccuracyWithFilter(PointListCustom plc_input,int index){
        // DCI = Down Confidence Interval , UCI = UP confidence Interval
        double SumAccuracy = 0, StandardDeviationAccuracy = 0, averageAccuracy = 0, variacne2 = 0, DCIAccuracy = 0, UCIAccuracy = 0;

        if(plc_input.size <= DynamicWindowSize){
            System.out.println("current point Accuracy:" + plc_input.getAccuracy(index));
            System.out.println(" ");
            return plc_input.getAccuracy(index) <= 99f;
        }
        else {

            if(plc_input.getAccuracy(index) > 100)
                return false;

            for(int i = index > DynamicWindowSize ? index - DynamicWindowSize : 0; index + DynamicWindowSize < plc_input.size() -1 ? i < index + DynamicWindowSize : i < plc_input.size(); i++){
                SumAccuracy += plc_input.getAccuracy(i);
            }

            //calc Average
            averageAccuracy = SumAccuracy / DynamicWindowSize;

            //calc Standard Deviation
            for(int n =  index > DynamicWindowSize ? index - DynamicWindowSize : 0; index + DynamicWindowSize < plc_input.size() -1 ? n < index + DynamicWindowSize : n < plc_input.size(); n++){
                variacne2 += Math.pow(plc_input.getAccuracy(n) - averageAccuracy,2);
            }

            StandardDeviationAccuracy = Math.sqrt(variacne2/DynamicWindowSize);

            //calc 95% Confidence interval CI = average +- 1.96 * (σ / √n), calc 99% CI =  average +- 2.58 * (σ / √n)
            DCIAccuracy = averageAccuracy - 1.96 *(StandardDeviationAccuracy / Math.sqrt(DynamicWindowSize));
            UCIAccuracy = averageAccuracy + 1.96 *(StandardDeviationAccuracy / Math.sqrt(DynamicWindowSize));

            System.out.println("Sd:" + StandardDeviationAccuracy + " Average:"+averageAccuracy);
            System.out.println("current point Accuracy:" + plc_input.getAccuracy(index));
            System.out.println("Accuracy Confidence Interval :" + DCIAccuracy + " ~ " + UCIAccuracy);
            System.out.println(" ");
            return plc_input.getAccuracy(index) <= UCIAccuracy;
        }
    }

    /**Filter Too Close to Point between interval**/
    private boolean TooClosePoint(PointListCustom plc_input,PointListCustom plc_correct,int index){

        double fromLat = plc_correct.getLat(plc_correct.size()-1);
        double fromLon = plc_correct.getLon(plc_correct.size()-1);
        double toLat = plc_input.getLat(index);
        double toLon = plc_input.getLon(index);

        if(15 < distanceCalcEarth.calcDist(fromLat,fromLon,toLat,toLon))
            SameFilterCount++;

        return  15 < distanceCalcEarth.calcDist(fromLat,fromLon,toLat,toLon);
    }

    public int getSamePointCount(){ return SameFilterCount; }

    /**Initial GPS Point List, Provide Robustness List*/
    public PointListCustom GpsInitWindows(PointListCustom InitPLC){
        // DCI = Down Confidence Interval , UCI = UP Confidence Interval
        double SumAccuracy = 0, StandardDeviationAccuracy = 0, averageAccuracy = 0, variacne2 = 0, DCIAccuracy = 0, UCIAccuracy = 0;
        double MoveDistance = 0;
        double AccCompares = 200;
        int index = 0;
        PointListCustom RobustnessPointList = new PointListCustom();

        for(int k =0; k < InitPLC.size(); k++){
            SumAccuracy += InitPLC.getAccuracy(k);
        }
        averageAccuracy = SumAccuracy / InitPLC.size();

        for(int j=0; j < InitPLC.size(); j++){
            variacne2 += Math.pow(InitPLC.getAccuracy(j) - averageAccuracy,2);
        }

        StandardDeviationAccuracy = Math.sqrt(variacne2/InitPLC.size());

        DCIAccuracy = averageAccuracy - 2.58 *(StandardDeviationAccuracy / Math.sqrt(24));
        UCIAccuracy = averageAccuracy + 2.58 *(StandardDeviationAccuracy / Math.sqrt(24));

        System.out.println("DCI :" + DCIAccuracy +"~ UCI : " + UCIAccuracy);

        for(int z=0; z < InitPLC.size()-1; z++){
            MoveDistance = distanceCalcEarth.calcDist(InitPLC.getLat(z),InitPLC.getLon(z),InitPLC.getLat(z+1),InitPLC.getLon(z+1)) ;

            if(InitPLC.getAccuracy(z) < UCIAccuracy && MoveDistance > 1)
                RobustnessPointList.add(InitPLC.getLat(z),InitPLC.getLon(z),InitPLC.getEle(z),InitPLC.getAccuracy(z),InitPLC.getTime(z));
            System.out.println("this list index:" +z +" Accuracy :"+InitPLC.getAccuracy(z) + " Distance: " + MoveDistance);
        }

        if(RobustnessPointList.size() < 1){
            for(int i=0; i < InitPLC.size(); i++){
                if(InitPLC.getAccuracy(i) < AccCompares)
                {
                    AccCompares = InitPLC.getAccuracy(i);
                    index = i;
                }
            }
            RobustnessPointList.add(InitPLC.getLat(index),InitPLC.getLon(index),InitPLC.getEle(index),InitPLC.getAccuracy(index),InitPLC.getTime(index));
        }

        System.out.println("Robustness Point List:" + RobustnessPointList);
        return RobustnessPointList;
    }

    /**detect stay place, it not use history gpx**/
    public PointListCustom PlaceStayCheck(PointListCustom plc_input){
        //detect by distance
        int CurrentGPXIndex = plc_input.size() - 1;
        int PreviousGPXIndex = plc_input.size() - 2;
        // distance from calc current place - previous place
        double distance = distanceCalcEarth.calcDist(plc_input.getLat(PreviousGPXIndex),plc_input.getLon(PreviousGPXIndex),plc_input.getLat(CurrentGPXIndex),plc_input.getLon(CurrentGPXIndex));
        double stayCheckDistance = 50;
        int stayCheckTime = 2;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PointListCustom plcStayPlace = new PointListCustom();

        //if distance < 40m
        if(distance < stayCheckDistance){
            //check to stay place when to start
            for(int TrackPrev = PreviousGPXIndex -1 ; TrackPrev > 0 ; TrackPrev--){

                double checkStartStay = distanceCalcEarth.calcDist(plc_input.getLat(TrackPrev),plc_input.getLon(TrackPrev),
                        plc_input.getLat(CurrentGPXIndex),plc_input.getLon(CurrentGPXIndex));

                // if true, the Previous Node maybe Stay Place
                if(checkStartStay > stayCheckDistance){
                    long PreviousTime =0;
                    long CurrentTime=0;
                    // time check , set time as 10 min
                    try {
                        PreviousTime = simpleDateFormat.parse(plc_input.getTime(TrackPrev+1)).getTime();
                        CurrentTime = simpleDateFormat.parse(plc_input.getTime(CurrentGPXIndex)).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    System.out.println("distance:" + checkStartStay +" PreviousTime: " + PreviousTime +" CurrentTime: " + CurrentGPXIndex);
                    //getTime() return 1970-01-01 00:00:00 to current ms ,so 1min = 1 * 60 *1000
                    if(Math.abs(CurrentTime-PreviousTime) > stayCheckTime * 60 *1000 && PreviousTime != 0 && CurrentTime != 0){
                        plcStayPlace.add(plc_input.getLat(CurrentGPXIndex),plc_input.getLon(CurrentGPXIndex),Double.NaN,0,plc_input.getTime(CurrentGPXIndex));
                        System.out.println("add Stay Node: " + plcStayPlace + "Stay Time:" + String.valueOf(Math.abs(CurrentTime-PreviousTime)/60000));
                        System.out.println(" ");
                    }
                    break;
                }
            }
        }

        return plcStayPlace;
    }

    /**filter stay place same point**/
    public PointListCustom SamePointFiltered(PointListCustom plc_stay){

        int FilterSameDistance = 80;
        int plc_stay_size = plc_stay.size();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for(int plc_start = 0; plc_start < plc_stay_size -1; plc_start++) {
            if(distanceCalcEarth.calcDist(plc_stay.getLat(plc_start),plc_stay.getLon(plc_start),plc_stay.getLat(plc_start+1),plc_stay.getLon(plc_start+1)) < FilterSameDistance){
                long startTime =0;
                long nextTime=0;

                try{
                    startTime =  simpleDateFormat.parse(plc_stay.getTime(plc_start)).getTime();
                    nextTime = simpleDateFormat.parse(plc_stay.getTime(plc_start+1)).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                // 2019/05/29 Test modify successful
                if(startTime < nextTime)
                    plc_stay.setPLC(plc_start,plc_stay.getLat(plc_start),plc_stay.getLon(plc_start), Double.NaN,0,plc_stay.getTime(plc_start+1));

                plc_stay.remove(plc_start+1);
                plc_stay_size = plc_stay.size();
                System.out.println("remove successful index :" + plc_start+1);
            }
        }

        return  plc_stay;
    }
}
