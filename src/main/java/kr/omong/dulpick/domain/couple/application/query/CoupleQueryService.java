package kr.omong.dulpick.domain.couple.application.query;

import kr.omong.dulpick.domain.couple.application.exception.InvalidConnectionCodeException;
import kr.omong.dulpick.domain.couple.application.exception.MemberAlreadyConnectedException;
import kr.omong.dulpick.domain.couple.application.exception.SelfConnectionNotAllowedException;
import kr.omong.dulpick.domain.couple.application.query.reader.CoupleConnectionReader;
import kr.omong.dulpick.domain.couple.application.query.view.ConnectionCodePreview;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.application.support.ConnectionAbusePreventionService;
import kr.omong.dulpick.domain.couple.application.support.ConnectionAbusePreventionService.AttemptPermit;
import kr.omong.dulpick.domain.couple.application.support.ConnectionCodeNormalizer;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionAttempt;
import kr.omong.dulpick.domain.couple.domain.ConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.member.application.exception.MemberProfileRequiredException;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoupleQueryService {

    private final CoupleConnectionReader coupleConnectionReader;
    private final ConnectionCodeNormalizer connectionCodeNormalizer;
    private final ConnectionCodeRepository connectionCodeRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ConnectionAbusePreventionService abusePreventionService;

    public CoupleQueryService(
            CoupleConnectionReader coupleConnectionReader,
            ConnectionCodeNormalizer connectionCodeNormalizer,
            ConnectionCodeRepository connectionCodeRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            MemberProfileRepository memberProfileRepository,
            ConnectionAbusePreventionService abusePreventionService
    ) {
        this.coupleConnectionReader = coupleConnectionReader;
        this.connectionCodeNormalizer = connectionCodeNormalizer;
        this.connectionCodeRepository = connectionCodeRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.abusePreventionService = abusePreventionService;
    }

    @Transactional(readOnly = true)
    public CoupleConnectionStatus getMyStatus(Long memberId) {
        return coupleConnectionReader.read(memberId);
    }

    public ConnectionCodePreview preview(Long memberId, String rawCode) {
        return preview(memberId, rawCode, "internal");
    }

    public ConnectionCodePreview preview(
            Long memberId,
            String rawCode,
            String clientAddress
    ) {
        AttemptPermit permit = abusePreventionService.begin(
                memberId,
                clientAddress,
                ConnectionAttempt.Action.PREVIEW
        );
        permit.requireAllowed();
        try {
            ConnectionCodePreview preview = readPreview(memberId, rawCode);
            abusePreventionService.completeSuccess(permit);
            return preview;
        } catch (BusinessException exception) {
            abusePreventionService.completeFailure(memberId, permit, exception);
            throw exception;
        }
    }

    private ConnectionCodePreview readPreview(Long memberId, String rawCode) {
        validateRequester(memberId);
        String code = connectionCodeNormalizer.normalize(rawCode);
        ConnectionCode connectionCode = connectionCodeRepository.findByCodeDigestAndStatus(
                Sha256.hex(code),
                ConnectionCodeStatus.ACTIVE
        ).orElseThrow(InvalidConnectionCodeException::new);
        Long inviterId = connectionCode.getMember().getId();
        if (memberId.equals(inviterId)) {
            throw new SelfConnectionNotAllowedException();
        }
        MemberProfile inviterProfile = memberProfileRepository.findById(inviterId)
                .orElseThrow(InvalidConnectionCodeException::new);
        return new ConnectionCodePreview(
                inviterProfile.getNickname(),
                inviterProfile.getProfileIcon()
        );
    }

    private void validateRequester(Long memberId) {
        if (!memberProfileRepository.existsById(memberId)) {
            throw new MemberProfileRequiredException();
        }
        if (activeCoupleMemberRepository.findByMemberId(memberId).isPresent()) {
            throw new MemberAlreadyConnectedException();
        }
    }
}
