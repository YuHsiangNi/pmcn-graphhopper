package com.graphhopper.Database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBSource {

    Connection getConnection() throws SQLException;

    void closeConnection(Connection connection) throws SQLException;
}
