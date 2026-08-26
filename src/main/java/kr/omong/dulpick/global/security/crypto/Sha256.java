package kr.omong.dulpick.global.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256 {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private Sha256() {
    }

    public static String hex(String value) {
        return HEX_FORMAT.formatHex(digest(value));
    }

    public static String hex(byte[] value) {
        return HEX_FORMAT.formatHex(digest(value));
    }

    private static byte[] digest(String value) {
        return digest(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] digest(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
