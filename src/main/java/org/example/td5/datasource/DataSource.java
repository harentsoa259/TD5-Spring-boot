
package org.example.td5.datasource;

import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.DriverManager;

@Component
public class DataSource {
    private final String url = "jdbc:postgresql://localhost:5432/td5_spring_boot";
    private final String user = "postgres";
    private final String password = "harentsoa";

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            System.err.println("Connection Error: " + e.getMessage());
            return null;
        }
    }
}