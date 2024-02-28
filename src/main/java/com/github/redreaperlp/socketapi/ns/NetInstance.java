package com.github.redreaperlp.socketapi.ns;

import com.github.redreaperlp.socketapi.communication.Connection;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public interface NetInstance {

    void notifyConnectionClosed(Connection con);

    byte[] encryptionKey();

    void setEncryptionKey(byte[] key);

    default void useEncryption(String key) {
        try {
            byte[] keyHash = MessageDigest.getInstance("SHA-256").digest(key.getBytes());
            setEncryptionKey(keyHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    default boolean usesEncrytion() {
        return encryptionKey() != null;
    }

    default String encrypt(String plaintext) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    default String decrypt(String ciphertext) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    void stop();
    boolean stopped();
}