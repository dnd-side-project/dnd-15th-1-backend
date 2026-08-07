package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.domain.ConnectionCodeFormat;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ConnectionCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final SecureRandom secureRandom;

    public ConnectionCodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        char[] code = new char[ConnectionCodeFormat.LENGTH];
        for (int index = 0; index < ConnectionCodeFormat.LENGTH; index++) {
            code[index] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
