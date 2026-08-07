package kr.omong.dulpick.domain.couple.infrastructure.crypto;

import kr.omong.dulpick.domain.couple.config.ConnectionCodeEncryptionProperties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConnectionCodeCipher {

    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final String activeKeyId;
    private final SecretKey activeKey;
    private final Map<String, SecretKey> keysById;
    private final List<SecretKey> legacyDecryptionKeys;
    private final SecureRandom secureRandom;

    public ConnectionCodeCipher(String base64Key, SecureRandom secureRandom) {
        this.activeKeyId = null;
        this.activeKey = createSecretKey(base64Key);
        this.keysById = Map.of();
        this.legacyDecryptionKeys = List.of(activeKey);
        this.secureRandom = secureRandom;
    }

    public ConnectionCodeCipher(
            ConnectionCodeEncryptionProperties properties,
            SecureRandom secureRandom
    ) {
        this.activeKeyId = properties.activeKeyId();
        this.activeKey = createSecretKey(properties.activeKey());
        Map<String, SecretKey> configuredKeys = new LinkedHashMap<>();
        configuredKeys.put(activeKeyId, activeKey);
        if (properties.hasPreviousKey()) {
            configuredKeys.put(
                    properties.previousKeyId(),
                    createSecretKey(properties.previousKey())
            );
        }
        this.keysById = Map.copyOf(configuredKeys);
        this.legacyDecryptionKeys = new ArrayList<>(configuredKeys.values());
        this.secureRandom = secureRandom;
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, activeKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
            return activeKeyId == null ? payload : activeKeyId + "." + payload;
        } catch (GeneralSecurityException exception) {
            throw new ConnectionCodeEncryptionException(exception);
        }
    }

    public String decrypt(String encryptedValue) {
        int separator = encryptedValue.indexOf('.');
        if (separator > 0) {
            return decryptVersioned(encryptedValue, separator);
        }
        return decryptLegacy(encryptedValue);
    }

    private String decryptVersioned(String encryptedValue, int separator) {
        String keyId = encryptedValue.substring(0, separator);
        SecretKey key = keysById.get(keyId);
        if (key == null) {
            throw new ConnectionCodeEncryptionException();
        }
        return decrypt(encryptedValue.substring(separator + 1), key);
    }

    private String decryptLegacy(String encryptedValue) {
        for (SecretKey key : legacyDecryptionKeys) {
            try {
                return decrypt(encryptedValue, key);
            } catch (ConnectionCodeEncryptionException ignored) {
            }
        }
        throw new ConnectionCodeEncryptionException();
    }

    private String decrypt(String encryptedValue, SecretKey key) {
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
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
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
