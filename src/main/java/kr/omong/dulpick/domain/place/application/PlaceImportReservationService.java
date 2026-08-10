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
import java.util.UUID;

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
        PlaceImport existing = importRepository
                .findByMemberIdAndCanonicalUrlHash(memberId, urlHash)
                .orElse(null);
        if (existing != null) {
            return new Reservation(existing.getId());
        }
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
        return new Reservation(placeImport.getId());
    }

    @Transactional
    public String claimPending(Long importId, Instant now, Instant staleBefore) {
        String claimToken = UUID.randomUUID().toString();
        return importRepository.claimPending(importId, claimToken, now, staleBefore) == 1
                ? claimToken
                : null;
    }

    @Transactional
    public boolean requeueRetryable(
            Long importId,
            Instant now,
            Instant staleBefore,
            Instant retryBefore
    ) {
        return importRepository.requeueRetryable(
                importId,
                now,
                staleBefore,
                retryBefore,
                properties.maxRetryCount()
        ) == 1;
    }

    @Transactional
    public boolean heartbeatClaim(Long importId, String claimToken, Instant now) {
        return importRepository.heartbeatClaim(importId, claimToken, now) == 1;
    }

    @Transactional
    public boolean requeueClaimed(Long importId, String claimToken, Instant now) {
        return importRepository.requeueClaimed(importId, claimToken, now) == 1;
    }

    @Transactional
    public boolean failClaimed(
            Long importId,
            String claimToken,
            String failureCode,
            Instant now
    ) {
        return importRepository.failClaimed(importId, claimToken, failureCode, now) == 1;
    }

    public record Reservation(Long importId) {
    }
}
