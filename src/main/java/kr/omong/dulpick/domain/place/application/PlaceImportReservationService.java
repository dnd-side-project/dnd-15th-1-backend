package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PlaceImportReservationService {

    private final PlaceImportRepository importRepository;

    public PlaceImportReservationService(PlaceImportRepository importRepository) {
        this.importRepository = importRepository;
    }

    @Transactional
    public Reservation reserve(
            Long memberId,
            String canonicalUrl,
            String urlHash,
            ContentSourceType sourceType,
            Instant now
    ) {
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

    public record Reservation(Long importId, boolean claimed) {
    }
}
