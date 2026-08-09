package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PlaceImportReservationService {

    private final PlaceImportRepository importRepository;
    private final MemberRepository memberRepository;
    private final PlaceAnalysisProperties properties;

    public PlaceImportReservationService(
            PlaceImportRepository importRepository,
            MemberRepository memberRepository,
            PlaceAnalysisProperties properties
    ) {
        this.importRepository = importRepository;
        this.memberRepository = memberRepository;
        this.properties = properties;
    }

    @Transactional
    public Reservation reserve(
            Long memberId,
            String canonicalUrl,
            String urlHash,
            ContentSourceType sourceType,
            Instant now
    ) {
        memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        Instant since = now.minusSeconds(24 * 60 * 60);
        if (importRepository.countByMemberIdAndCreatedAtGreaterThanEqual(memberId, since)
                >= properties.dailyLimit()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        importRepository.insertIfAbsent(
                memberId,
                canonicalUrl,
                urlHash,
                sourceType.name(),
                now
        );
        PlaceImport placeImport = importRepository
                .findByMemberIdAndCanonicalUrlHash(memberId, urlHash)
                .orElseThrow(IllegalStateException::new);
        boolean claimed = importRepository.claimReceived(placeImport.getId(), now) == 1;
        return new Reservation(placeImport.getId(), claimed);
    }

    @Transactional
    public boolean claimRetryable(Long importId, Instant now, Instant staleBefore) {
        return importRepository.claimRetryable(importId, now, staleBefore) == 1;
    }

    @Transactional
    public boolean claimChangedCompleted(Long importId, Instant now) {
        return importRepository.claimChangedCompleted(importId, now) == 1;
    }

    public record Reservation(Long importId, boolean claimed) {
    }
}
