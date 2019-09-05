package com.graphhopper.GPXUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *Training weight
 *
 author Yu-Hsiang Lin
 **/


public class GPXTraining {

    /**if training , use edge**/
    public double TrainWeighting(double CurrentWeighting, String PerTime){
        double EdgeWeighting = 0;

        EdgeWeighting = (1 - AbnormalSigmoid(PerTime)) * 3  +  AbnormalSigmoid(PerTime) * CurrentWeighting;

        return EdgeWeighting;
    }
    /**training , not use**/
    public double WeakeningWeighting(double CurrentWeighting, String PerTime){
        double edgeWeighting = 0;

        edgeWeighting = AbnormalSigmoid(PerTime) * CurrentWeighting;

        return  edgeWeighting;
    }


    /**time recovery function**/
    private double AbnormalSigmoid(String PerTime){
        double Riscore = 0 , Ti = 0;
        long CurrentTime =0, LastTime=0;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date newTime = new Date();
        String nowTime = simpleDateFormat.format(newTime);

        try{
            CurrentTime = simpleDateFormat.parse(nowTime).getTime();
            LastTime = simpleDateFormat.parse(PerTime).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Ti = 5 - (Math.log10((Math.abs(CurrentTime-LastTime)/1000) / 3600) / 1.5);

        Riscore = 1  /  (1 + Math.exp(-Ti));

        System.out.println("Ti time recovery function: " + Ti + "time score: " + Riscore);

        return  Riscore;
    }

}
