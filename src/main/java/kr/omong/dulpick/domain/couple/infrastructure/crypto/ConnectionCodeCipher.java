package kr.omong.dulpick.domain.couple.infrastructure.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class ConnectionCodeCipher {

    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public ConnectionCodeCipher(String base64Key, SecureRandom secureRandom) {
        this.secretKey = createSecretKey(base64Key);
        this.secureRandom = secureRandom;
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (GeneralSecurityException exception) {
            throw new ConnectionCodeEncryptionException(exception);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] combined = Base64.getUrlDecoder().decode(encryptedValue);
            if (combined.length <= IV_BYTES) {
                throw new ConnectionCodeEncryptionException();
            }
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new ConnectionCodeEncryptionException(exception);
        }
    }

    private SecretKey createSecretKey(String base64Key) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            if (key.length != 32) {
                throw new ConnectionCodeEncryptionException();
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new ConnectionCodeEncryptionException(exception);
        }
    }
}
