package com.eclinic.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * JWT utility for generating and verifying authentication tokens.
 */
public class JwtUtil {

    private static String SECRET;
    private static final long EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours

    static {
        // Try to load secret from environment, otherwise generate a secure random one
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && envSecret.length() >= 32) {
            SECRET = envSecret;
        } else {
            String propSecret = System.getProperty("jwt.secret");
            if (propSecret != null && propSecret.length() >= 32) {
                SECRET = propSecret;
            } else {
                byte[] key = new byte[64];
                new SecureRandom().nextBytes(key);
                SECRET = Base64.getEncoder().encodeToString(key);
                System.out.println("WARNING: JWT_SECRET not set. Using auto-generated key. Tokens will invalidate on restart.");
            }
        }
    }

    /**
     * Generate a JWT token for an authenticated user.
     */
    public static String generateToken(long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    /**
     * Verify a JWT token and return the user ID, or null if invalid/expired.
     */
    public static Long verifyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract the role claim from a JWT token.
     */
    public static String extractRole(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract the username claim from a JWT token.
     */
    public static String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("username", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
