package com.quickmart.pos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DB {
    private DB() {}
    private static final String URL = System.getenv().getOrDefault(
        "QUICKMART_DB_URL",
        "jdbc:mysql://localhost:3306/quickmart_pos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila");
    private static final String USER = System.getenv().getOrDefault("QUICKMART_DB_USER", "quickmart_app");
    private static final String PASSWORD = System.getenv().getOrDefault("QUICKMART_DB_PASSWORD", "ChangeThis_QM_2026!");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
