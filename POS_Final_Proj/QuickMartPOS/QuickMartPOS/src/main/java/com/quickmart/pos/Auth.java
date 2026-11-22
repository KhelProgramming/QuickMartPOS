package com.quickmart.pos;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Auth {
    private Auth() {}
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;

    public record User(int id, String username, String fullName, String role) {}

    public static String hash(String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] key = derive(password.toCharArray(), salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(key);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verify(String password, String stored) throws Exception {
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] actual = derive(password.toCharArray(), Base64.getDecoder().decode(parts[1]),
                Integer.parseInt(parts[0]));
        return MessageDigest.isEqual(expected, actual);
    }

    public static User login(String username, String password) throws Exception {
        String sql = "SELECT id,username,full_name,role,password_hash FROM users WHERE username=? AND active=1";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !verify(password, rs.getString("password_hash"))) return null;
                return new User(rs.getInt("id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("role"));
            }
        }
    }
}
