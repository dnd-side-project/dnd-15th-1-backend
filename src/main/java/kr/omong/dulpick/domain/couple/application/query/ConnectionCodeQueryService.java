package kr.omong.dulpick.domain.couple.application.query;

import kr.omong.dulpick.domain.couple.application.exception.ConnectionCodeNotAvailableException;
import kr.omong.dulpick.domain.couple.application.support.ConnectionCodeIssuer;
import kr.omong.dulpick.domain.couple.application.support.IssuedConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.member.application.exception.MemberProfileRequiredException;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConnectionCodeQueryService {

    private final MemberProfileRepository memberProfileRepository;
    private final ConnectionCodeRepository connectionCodeRepository;
    private final ConnectionCodeIssuer connectionCodeIssuer;

    public ConnectionCodeQueryService(
            MemberProfileRepository memberProfileRepository,
            ConnectionCodeRepository connectionCodeRepository,
            ConnectionCodeIssuer connectionCodeIssuer
    ) {
        this.memberProfileRepository = memberProfileRepository;
        this.connectionCodeRepository = connectionCodeRepository;
        this.connectionCodeIssuer = connectionCodeIssuer;
    }

    public IssuedConnectionCode getMyActiveCode(Long memberId) {
        if (!memberProfileRepository.existsById(memberId)) {
            throw new MemberProfileRequiredException();
        }
        ConnectionCode code = connectionCodeRepository.findByMemberIdAndStatus(
                memberId,
                ConnectionCodeStatus.ACTIVE
        ).orElseThrow(ConnectionCodeNotAvailableException::new);
        return connectionCodeIssuer.read(code);
    }
}
