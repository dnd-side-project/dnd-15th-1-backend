package kr.omong.dulpick.domain.notification.infrastructure;

import kr.omong.dulpick.domain.notification.config.PushProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PushRegistrationCipher {

    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public PushRegistrationCipher(PushProperties properties, SecureRandom secureRandom) {
        this.secretKey = createConfiguredSecretKey(
                properties.registrationEncryptionKey()
        );
        this.secureRandom = secureRandom;
    }

    public String encrypt(String registrationId) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    requireSecretKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );
            byte[] encrypted = cipher.doFinal(
                    registrationId.getBytes(StandardCharsets.UTF_8)
            );
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new PushRegistrationEncryptionException(exception);
        }
    }

    public String decrypt(String encryptedRegistrationId) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedRegistrationId);
            if (payload.length <= IV_BYTES) {
                throw new PushRegistrationEncryptionException();
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    requireSecretKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new PushRegistrationEncryptionException(exception);
        }
    }

    public void requireConfigured() {
        if (secretKey == null) {
            throw new IllegalStateException(
                    "PUSH_REGISTRATION_ENCRYPTION_KEY is required"
            );
        }
    }

    private SecretKey requireSecretKey() {
        if (secretKey == null) {
            throw new PushRegistrationEncryptionException();
        }
        return secretKey;
    }

    private SecretKey createConfiguredSecretKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return null;
        }
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            if (key.length != 32) {
                throw new PushRegistrationEncryptionException();
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new PushRegistrationEncryptionException(exception);
        }
    }
}
