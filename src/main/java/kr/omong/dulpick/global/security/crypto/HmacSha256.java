package kr.omong.dulpick.global.security.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

public final class HmacSha256 {

    private static final String ALGORITHM = "HmacSHA256";
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private HmacSha256() {
    }

    public static String hex(String key, String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HEX_FORMAT.formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA-256 is not available", exception);
        }
    }
}
