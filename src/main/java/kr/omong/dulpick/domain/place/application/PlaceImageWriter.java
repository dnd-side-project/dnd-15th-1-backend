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
    private final PlaceImageStorageService storageService;
    private final Clock clock;

    public PlaceImageWriter(
            PlaceImageRepository placeImageRepository,
            PlaceRepository placeRepository,
            PlaceImageStorageService storageService,
            Clock clock
    ) {
        this.placeImageRepository = placeImageRepository;
        this.placeRepository = placeRepository;
        this.storageService = storageService;
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
        List<StoredPlaceImage> storedImages = limitedImageUrls.stream()
                .map(this::store)
                .flatMap(java.util.Optional::stream)
                .toList();
        if (storedImages.isEmpty()) {
            return;
        }
        placeImageRepository.deleteAllByPlaceId(placeId);
        List<PlaceImage> images = IntStream.range(0, storedImages.size())
                .mapToObj(index -> {
                    StoredPlaceImage stored = storedImages.get(index);
                    return PlaceImage.createStored(
                            placeId,
                            storageService.publicUrl(stored.image().storageKey()),
                            Sha256.hex(stored.sourceUrl()),
                            stored.image().storageKey(),
                            stored.image().contentType().toString(),
                            index,
                            now
                    );
                })
                .toList();
        placeImageRepository.saveAll(images);
        placeRepository.updateThumbnail(placeId, images.getFirst().getImageUrl(), now);
    }

    private java.util.Optional<StoredPlaceImage> store(String sourceUrl) {
        try {
            return java.util.Optional.of(new StoredPlaceImage(sourceUrl, storageService.store(sourceUrl)));
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    private record StoredPlaceImage(String sourceUrl, PlaceImageStorageService.StoredImage image) {
    }
}
