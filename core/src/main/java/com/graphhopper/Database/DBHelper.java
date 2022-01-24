package com.graphhopper.Database;

import com.graphhopper.GPXUtil.GPXTraining;
import com.graphhopper.GraphHopper;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

/**
 author Yu-Hsiang Lin
 **/


public class DBHelper {

    private GetDBSource dbSource;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private Statement statement;
    private GPXTraining gpxTraining = new GPXTraining();


    public void DBConnection() {

        try{
            dbSource = new GetDBSource();
            connection = dbSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);

            if(!connection.isClosed()) {
                System.out.println("database open...");
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void DBClose() throws SQLException {

        dbSource.closeConnection(connection);

        if(connection.isClosed()) {
            System.out.println("database close...");
        }
    }

    private void DBWrite(int edgeId, String time){
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO gpx VALUES(?,?,?,?,?)");

            preparedStatement.setInt(1,edgeId);
            preparedStatement.setDouble(2,0.4);
            preparedStatement.setInt(3,1);
            preparedStatement.setInt(4,1);
            preparedStatement.setString(5,time);

            preparedStatement.executeUpdate();
            preparedStatement.clearParameters();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**get Current Training Times**/
    public int getTrainingTimes(){
        int times = 0,DBtimes;

        try {
            ResultSet resultSet = statement.executeQuery("SELECT CurrentTrain FROM gpx");

            while (resultSet.next()){
                DBtimes = resultSet.getInt("CurrentTrain");
                if(DBtimes > times)
                    times = DBtimes;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return times;
    }

    /**get DB all Edge ID**/
    public ArrayList<Integer> getEdge(ArrayList<Integer> Training_Edge_ID){
        int id = 0, TrainingID = 0, ALL_ID = 0;
        boolean exist = false;
        ArrayList<Integer> ALL_Edge_ID = new ArrayList<>();
        ArrayList<Integer> Not_Use_Edge = new ArrayList<>();

        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM gpx");

            while (resultSet.next()){
                id = resultSet.getInt("edge");
                ALL_Edge_ID.add(id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (int i = 0  ; i < ALL_Edge_ID.size() ; i++){
            ALL_ID = ALL_Edge_ID.get(i);
            for(int j = 0 ; j < Training_Edge_ID.size() ; j++){
                TrainingID = Training_Edge_ID.get(j);
                if (ALL_ID == TrainingID)
                    exist = true;
            }

            if (!exist){
                Not_Use_Edge.add(ALL_ID);
            }
            exist = false;
        }
        System.out.println("Not Training Edge List: "+Not_Use_Edge);
        System.out.println(" ");

        return Not_Use_Edge;
    }


    /**Read data example**/
    public void DBRead(){
        try {
            ResultSet resultSet = preparedStatement.executeQuery("SELECT * FROM gpx");

            while (resultSet.next()){
                System.out.print(resultSet.getInt(1) + "\t");
                System.out.print(resultSet.getDouble(2) + "\t");
                System.out.print(resultSet.getInt(3) + "\t");
                System.out.print(resultSet.getInt(4) + "\t");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void TrainEdgeWeighting(ArrayList<Integer> EdgeID, String nowTime){

        boolean DataExist = false;
        double EdgeWeighting;
        int Times = getTrainingTimes();
        String Record;

        ArrayList<Integer> ReadyWeakeningEdge = new ArrayList<>();

        for (Integer integer : EdgeID) {
            try {
                ResultSet resultSet = statement.executeQuery("SELECT * FROM gpx WHERE edge =" + integer);
                while (resultSet.next()) {
                    System.out.println("Edge: " + integer);
                    EdgeWeighting = resultSet.getDouble("weighting");
                    Record = resultSet.getString("ReTime");
                    EdgeWeighting = gpxTraining.TrainWeighting(EdgeWeighting, Record);
                    DBUpdate(resultSet.getInt("edge"), EdgeWeighting, Times + 1, Times + 1, nowTime);
                    DataExist = true;
                }

                if (!DataExist) {
                    DBWrite(integer, nowTime);
                }
                DataExist = false;

            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        ReadyWeakeningEdge = getEdge(EdgeID);

        for(Integer edge : ReadyWeakeningEdge){
            try {
                ResultSet resultSet = statement.executeQuery("SELECT * FROM gpx WHERE edge =" + edge);
                while (resultSet.next()) {
                    System.out.println("Edge: " + edge);
                    EdgeWeighting = resultSet.getDouble("weighting");
                    Record = resultSet.getString("ReTime");
                    EdgeWeighting = gpxTraining.WeakeningWeighting(EdgeWeighting, Record);
                    DBUpdate(resultSet.getInt("edge"), EdgeWeighting, Times + 1,resultSet.getInt("LastTrain"), resultSet.getString("ReTime"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void DBUpdate(int edgeId, double weight, int current_time, int last_time, String record){
        try {
            preparedStatement = connection.prepareStatement("UPDATE gpx SET edge=?, weighting=?, CurrentTrain=?, LastTrain=?, ReTime=? WHERE edge="+ edgeId);

            preparedStatement.setInt(1,edgeId);
            preparedStatement.setDouble(2,weight);
            preparedStatement.setInt(3,current_time);
            preparedStatement.setInt(4,last_time);
            preparedStatement.setString(5,record);

            preparedStatement.executeUpdate();
            preparedStatement.clearParameters();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double DBGetEdgeWeighting(int EdgeID){

        double EdgeWeighting = 0;
        boolean DataExist = false;

        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM gpx WHERE edge =" + EdgeID);

            while(resultSet.next()){
                EdgeWeighting = resultSet.getDouble("weighting");
                DataExist = true;
            }

            if(!DataExist)
                return EdgeWeighting;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return EdgeWeighting;
    }

    /**storage stay place **/
    public void StorageSatyPlace(double lat, double lon, int edge){
        boolean DataExist = false;

        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM stayplace WHERE edge =" + edge +" AND (lat =" + lat +" AND lon = " + lon +")");

            while (resultSet.next()){
                UPDateStayPlace(lat,lon,resultSet.getInt("frequency")+1,edge);
                DataExist = true;
            }

            if(!DataExist){
                InitStayPlace(lat,lon,edge);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**if data is not exist, add init data**/
    private void InitStayPlace(double lat, double lon, int edge){
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO stayplace VALUES(?,?,?,?)");

            preparedStatement.setDouble(1,lat);
            preparedStatement.setDouble(2,lon);
            preparedStatement.setInt(3,1);
            preparedStatement.setInt(4,edge);

            preparedStatement.executeUpdate();
            preparedStatement.clearParameters();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**update the same Stay Place frequency **/
    private void UPDateStayPlace(double lat, double lon, int frequency ,int edge){
        try {
            preparedStatement = connection.prepareStatement("UPDATE stayplace SET frequency=? WHERE edge =" + edge +" AND (lat =" + lat +" AND lon = " + lon +")");

            preparedStatement.setInt(1,frequency);

            preparedStatement.executeUpdate();
            preparedStatement.clearParameters();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**Get Stay Place Frequency on Edge**/
    public int GetEdgeStayFrequency(int edge){

        int Frequency = 0;
        boolean DataExist = false;

        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM stayplace WHERE edge =" + edge);

            while(resultSet.next()){
                Frequency = Frequency + resultSet.getInt("frequency");
                DataExist = true;
            }

            if(!DataExist)
                return Frequency;


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Frequency;

    }


}
