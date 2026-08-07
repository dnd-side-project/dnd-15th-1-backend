package kr.omong.dulpick.domain.couple.application.support;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ConnectionCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 6;

    private final SecureRandom secureRandom;

    public ConnectionCodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        char[] code = new char[CODE_LENGTH];
        for (int index = 0; index < CODE_LENGTH; index++) {
            code[index] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
