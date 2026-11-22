package com.quickmart.pos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                initializeDefaults();
                new LoginFrame().setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Cannot start QuickMart POS.\n" + ex.getMessage()
                    + "\n\nCheck MySQL and execute database.sql first.",
                    "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void initializeDefaults() throws Exception {
        try (Connection c = DB.getConnection()) {
            int count;
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                count = rs.getInt(1);
            }
            if (count == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(username,full_name,role,active,password_hash) VALUES(?,?,?,?,?)")) {
                    ps.setString(1, "admin");
                    ps.setString(2, "QuickMart Administrator");
                    ps.setString(3, "ADMIN");
                    ps.setBoolean(4, true);
                    ps.setString(5, Auth.hash("admin123"));
                    ps.executeUpdate();

                    ps.setString(1, "cashier");
                    ps.setString(2, "Default Cashier");
                    ps.setString(3, "CASHIER");
                    ps.setBoolean(4, true);
                    ps.setString(5, Auth.hash("cashier123"));
                    ps.executeUpdate();
                }
            }
        }
    }
}
