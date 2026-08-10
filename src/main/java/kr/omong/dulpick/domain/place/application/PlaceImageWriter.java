package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceImage;
import kr.omong.dulpick.domain.place.domain.PlaceImageRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class PlaceImageWriter {

    private final PlaceImageRepository placeImageRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    public PlaceImageWriter(
            PlaceImageRepository placeImageRepository,
            PlaceRepository placeRepository,
            Clock clock
    ) {
        this.placeImageRepository = placeImageRepository;
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    @Transactional
    public void replace(Long placeId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        List<String> limitedImageUrls = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .limit(5)
                .toList();
        if (limitedImageUrls.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        String thumbnailUrl = limitedImageUrls.getFirst();
        List<String> detailImageUrls = limitedImageUrls.subList(1, limitedImageUrls.size());
        placeImageRepository.deleteAllByPlaceId(placeId);
        placeImageRepository.saveAll(IntStream.range(0, detailImageUrls.size())
                .mapToObj(index -> PlaceImage.create(
                        placeId,
                        detailImageUrls.get(index),
                        Sha256.hex(detailImageUrls.get(index)),
                        index,
                        now
                ))
                .toList());
        placeRepository.updateThumbnail(placeId, thumbnailUrl, now);
    }
}
