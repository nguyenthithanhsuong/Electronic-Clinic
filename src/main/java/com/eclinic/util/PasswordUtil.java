package com.eclinic.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing utility using bcrypt.
 * Includes fallback for legacy plaintext passwords during migration.
 */
public class PasswordUtil {

    /**
     * Hash a plaintext password with bcrypt.
     */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Check a plaintext password against a stored hash.
     * Supports both bcrypt hashes and legacy plaintext (for migration).
     */
    public static boolean check(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        // Bcrypt hashes start with "$2a$", "$2b$", or "$2y$"
        if (storedHash.startsWith("$2")) {
            return BCrypt.checkpw(plainPassword, storedHash);
        }
        // Legacy plaintext comparison (migration path)
        return plainPassword.equals(storedHash);
    }

    /**
     * Check if a stored hash is bcrypt or legacy plaintext.
     */
    public static boolean isBcrypt(String storedHash) {
        return storedHash != null && storedHash.startsWith("$2");
    }
}
