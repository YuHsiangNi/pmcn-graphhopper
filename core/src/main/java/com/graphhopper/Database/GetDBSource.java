package com.graphhopper.Database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 author Yu-Hsiang Lin
 **/

public class GetDBSource implements DBSource{
    private String url;
    private String user;
    private String passwd;

     GetDBSource() throws IOException,ClassCastException{
        this("jdbc.properties");
    }

    private GetDBSource(String configFile) throws IOException,ClassCastException{
        Properties props = new Properties();
        props.load(new FileInputStream(configFile));

        url = props.getProperty("com.graphhopper.Database.url");
        user = props.getProperty("com.graphhopper.Database.user");
        passwd = props.getProperty("com.graphhopper.Database.password");

        try {
            Class.forName(props.getProperty("com.graphhopper.Database.driver"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException{
        return DriverManager.getConnection(url, user, passwd);
    }

    public void closeConnection(Connection conn) throws SQLException {
        conn.close();
    }
}
