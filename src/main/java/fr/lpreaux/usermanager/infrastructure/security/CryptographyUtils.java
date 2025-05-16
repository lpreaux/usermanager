package fr.lpreaux.usermanager.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utilitaire pour les opérations cryptographiques.
 * Fournit des méthodes pour le hachage, le chiffrement/déchiffrement, et la génération
 * de valeurs aléatoires sécurisées.
 */
@Component
@Slf4j
public class CryptographyUtils {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Hache une chaîne avec SHA-256.
     *
     * @param input La chaîne à hacher
     * @return Le hachage en hexadécimal
     */
    public String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error while hashing with SHA-256", e);
            throw new SecurityException("Hashing error", e);
        }
    }

    /**
     * Génère un hachage sécurisé avec sel et PBKDF2.
     * Format du résultat: base64(salt):base64(hash)
     *
     * @param input La chaîne à hacher
     * @return Le hachage sécurisé avec le sel
     */
    public String secureHash(String input) {
        try {
            byte[] salt = generateRandomBytes(SALT_LENGTH);
            byte[] hash = pbkdf2(input.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH / 8);

            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            return saltBase64 + ":" + hashBase64;
        } catch (Exception e) {
            log.error("Error while creating secure hash", e);
            throw new SecurityException("Secure hashing error", e);
        }
    }

    /**
     * Vérifie si un mot de passe correspond à un hachage sécurisé.
     *
     * @param input Le mot de passe à vérifier
     * @param storedHash Le hachage stocké au format base64(salt):base64(hash)
     * @return true si le mot de passe correspond, false sinon
     */
    public boolean verifySecureHash(String input, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] hash = Base64.getDecoder().decode(parts[1]);

            byte[] testHash = pbkdf2(input.toCharArray(), salt, ITERATION_COUNT, hash.length);

            return MessageDigest.isEqual(hash, testHash);
        } catch (Exception e) {
            log.error("Error while verifying secure hash", e);
            return false;
        }
    }

    /**
     * Chiffre une chaîne avec AES.
     * Format du résultat: base64(iv):base64(encrypted)
     *
     * @param input La chaîne à chiffrer
     * @param key La clé de chiffrement
     * @return La chaîne chiffrée avec le vecteur d'initialisation
     */
    public String encrypt(String input, String key) {
        try {
            byte[] iv = generateRandomBytes(IV_LENGTH);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            SecretKey secretKey = generateAESKey(key);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);

            return ivBase64 + ":" + encryptedBase64;
        } catch (Exception e) {
            log.error("Error while encrypting", e);
            throw new SecurityException("Encryption error", e);
        }
    }

    /**
     * Déchiffre une chaîne AES.
     *
     * @param input La chaîne chiffrée au format base64(iv):base64(encrypted)
     * @param key La clé de déchiffrement
     * @return La chaîne déchiffrée
     */
    public String decrypt(String input, String key) {
        try {
            String[] parts = input.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted string format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            SecretKey secretKey = generateAESKey(key);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error while decrypting", e);
            throw new SecurityException("Decryption error", e);
        }
    }

    /**
     * Génère un token sécurisé aléatoire.
     *
     * @param length Longueur du token en octets
     * @return Le token en Base64
     */
    public String generateSecureToken(int length) {
        byte[] token = generateRandomBytes(length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    /**
     * Génère un sel aléatoire.
     *
     * @return Le sel en Base64
     */
    public String generateSalt() {
        byte[] salt = generateRandomBytes(SALT_LENGTH);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Méthodes utilitaires privées

    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    private SecretKey generateAESKey(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Utiliser le mot de passe pour dériver une clé AES
        byte[] salt = "UserManagerSecretKey".getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = pbkdf2(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH / 8);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}