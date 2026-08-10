package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.place.application.exception.PlaceAnalysisUnavailableException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class PlaceImportService {

    private final MemberRepository memberRepository;
    private final PlaceImportRepository importRepository;
    private final PlaceImportViewMapper viewMapper;
    private final PlaceImportReservationService reservationService;
    private final ContentSourceUrlParser urlParser;
    private final PlaceAnalysisProperties properties;
    private final Clock clock;

    public PlaceImportService(
            MemberRepository memberRepository,
            PlaceImportRepository importRepository,
            PlaceImportViewMapper viewMapper,
            PlaceImportReservationService reservationService,
            ContentSourceUrlParser urlParser,
            PlaceAnalysisProperties properties,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.importRepository = importRepository;
        this.viewMapper = viewMapper;
        this.reservationService = reservationService;
        this.urlParser = urlParser;
        this.properties = properties;
        this.clock = clock;
    }

    public PlaceImportSubmissionView importLink(Long memberId, String rawUrl) {
        if (!properties.enabled()) {
            throw new PlaceAnalysisUnavailableException(null);
        }
        Member member = findActiveMember(memberId);
        ContentSourceUrlParser.ParsedSource source = urlParser.parse(rawUrl);
        String urlHash = Sha256.hex(source.canonicalUrl());
        PlaceImport existing = importRepository
                .findByMemberIdAndCanonicalUrlHash(memberId, urlHash)
                .orElse(null);
        if (existing != null) {
            if (canRetry(existing)
                    && reservationService.requeueRetryable(
                    existing.getId(),
                    clock.instant(),
                    clock.instant().minusSeconds(properties.staleTimeoutSeconds()),
                    clock.instant().minusSeconds(properties.retryCooldownSeconds()))) {
                existing = reload(existing);
            }
            return new PlaceImportSubmissionView(viewMapper.toView(existing));
        }
        Instant now = clock.instant();
        PlaceImportReservationService.Reservation reservation = reservationService.reserve(
                member.getId(),
                source.canonicalUrl(),
                urlHash,
                source.sourceType(),
                now
        );
        PlaceImport placeImport = importRepository.findById(reservation.importId())
                .orElseThrow(IllegalStateException::new);
        return new PlaceImportSubmissionView(viewMapper.toView(placeImport));
    }

    private boolean canRetry(PlaceImport placeImport) {
        if (placeImport.getStatus() == PlaceImportStatus.FAILED
                || placeImport.getStatus() == PlaceImportStatus.RECEIVED) {
            if (placeImport.getStatus() == PlaceImportStatus.RECEIVED) {
                return true;
            }
            return placeImport.getRetryCount() < properties.maxRetryCount()
                    && placeImport.getUpdatedAt()
                    .plusSeconds(properties.retryCooldownSeconds())
                    .isBefore(clock.instant());
        }
        if (placeImport.getStatus() != PlaceImportStatus.PROCESSING) {
            return false;
        }
        return placeImport.getUpdatedAt().plusSeconds(properties.staleTimeoutSeconds())
                .isBefore(clock.instant());
    }

    private Member findActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return member;
    }

    private PlaceImport reload(PlaceImport placeImport) {
        return importRepository.findById(placeImport.getId()).orElse(placeImport);
    }
}
