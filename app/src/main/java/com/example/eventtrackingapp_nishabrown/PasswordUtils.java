package com.example.eventtrackingapp_nishabrown;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Provides secure password hashing and verification for DateMate.
 * Uses PBKDF2 with a randomly generated salt.
 */
public final class PasswordUtils {

    private static final int ITERATIONS = 210000;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordUtils() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Generates a salted password hash for a new account.
     *
     * @param password Plain-text password entered by the user.
     * @return Encoded password information for database storage.
     */
    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] hash = generateHash(password, salt, ITERATIONS);

        return ITERATIONS + ":" +
                Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a password against its stored hash.
     *
     * @param password Password entered during login.
     * @param storedHash Previously stored password hash.
     * @return true when the password matches.
     */
    public static boolean verifyPassword(
            String password,
            String storedHash
    ) {
        try {
            String[] parts = storedHash.split(":");

            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] originalHash = Base64.getDecoder().decode(parts[2]);

            if (iterations < 1 || iterations > 1000000 ||
                    salt.length != SALT_LENGTH ||
                    originalHash.length != HASH_LENGTH / 8) {
                return false;
            }

            byte[] testHash = generateHash(password, salt, iterations);

            return MessageDigest.isEqual(originalHash, testHash);

        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Generates a PBKDF2 hash using the supplied salt and iteration count.
     */
    private static byte[] generateHash(
            String password,
            byte[] salt,
            int iterations
    ) {
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                HASH_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(ALGORITHM);

            return factory.generateSecret(spec).getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to generate password hash.", e
            );
        } finally {
            ((PBEKeySpec) spec).clearPassword();
        }
    }
}
