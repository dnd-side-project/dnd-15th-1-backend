package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.config.CoupleProperties;
import kr.omong.dulpick.domain.couple.domain.ConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeIssuedReason;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.infrastructure.crypto.ConnectionCodeCipher;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class ConnectionCodeIssuer {

    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final ConnectionCodeRepository connectionCodeRepository;
    private final ConnectionCodeGenerator connectionCodeGenerator;
    private final ConnectionCodeCipher connectionCodeCipher;
    private final CoupleProperties properties;
    private final Clock clock;

    public ConnectionCodeIssuer(
            ConnectionCodeRepository connectionCodeRepository,
            ConnectionCodeGenerator connectionCodeGenerator,
            ConnectionCodeCipher connectionCodeCipher,
            CoupleProperties properties,
            Clock clock
    ) {
        this.connectionCodeRepository = connectionCodeRepository;
        this.connectionCodeGenerator = connectionCodeGenerator;
        this.connectionCodeCipher = connectionCodeCipher;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedConnectionCode issue(
            Member member,
            ConnectionCodeIssuedReason issuedReason
    ) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = connectionCodeGenerator.generate();
            String digest = Sha256.hex(code);
            if (connectionCodeRepository.existsByCodeDigest(digest)) {
                continue;
            }
            ConnectionCode connectionCode = ConnectionCode.issue(
                    member,
                    digest,
                    connectionCodeCipher.encrypt(code),
                    issuedReason,
                    clock.instant()
            );
            connectionCodeRepository.save(connectionCode);
            return new IssuedConnectionCode(code, createShareUrl(code));
        }
        throw new IllegalStateException("Failed to generate a unique connection code");
    }

    public IssuedConnectionCode read(ConnectionCode connectionCode) {
        String code = connectionCodeCipher.decrypt(connectionCode.getEncryptedCode());
        return new IssuedConnectionCode(code, createShareUrl(code));
    }

    private String createShareUrl(String code) {
        String baseUrl = properties.publicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/connect?code=" + code;
    }
}
