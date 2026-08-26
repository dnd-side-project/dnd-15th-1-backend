package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceImage;
import kr.omong.dulpick.domain.place.domain.PlaceImageRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class PlaceImageWriter {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImageWriter.class);

    private final PlaceImageRepository placeImageRepository;
    private final PlaceRepository placeRepository;
    private final PlaceImageStorageService storageService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public PlaceImageWriter(
            PlaceImageRepository placeImageRepository,
            PlaceRepository placeRepository,
            PlaceImageStorageService storageService,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.placeImageRepository = placeImageRepository;
        this.placeRepository = placeRepository;
        this.storageService = storageService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public boolean replace(Long placeId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return false;
        }
        List<String> limitedImageUrls = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .limit(5)
                .toList();
        if (limitedImageUrls.isEmpty()) {
            return false;
        }
        Instant now = clock.instant();
        List<StoredPlaceImage> storedImages = downloadAll(limitedImageUrls);
        if (storedImages.isEmpty()) {
            logger.warn("place_image_storage_failed placeId={} reason=ALL_DOWNLOADS_FAILED", placeId);
            return false;
        }
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
        try {
            transactionTemplate.executeWithoutResult(status -> replaceRows(placeId, images, now));
            return true;
        } catch (RuntimeException exception) {
            images.forEach(image -> deleteStoredFile(image.getStorageKey()));
            throw exception;
        }
    }

    private List<StoredPlaceImage> downloadAll(List<String> imageUrls) {
        return imageUrls.stream()
                .map(this::store)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private void replaceRows(Long placeId, List<PlaceImage> images, Instant now) {
        placeImageRepository.deleteAllByPlaceId(placeId);
        placeImageRepository.saveAll(images);
        placeRepository.updateThumbnail(placeId, images.getFirst().getImageUrl(), now);
    }

    private java.util.Optional<StoredPlaceImage> store(String sourceUrl) {
        try {
            return java.util.Optional.of(new StoredPlaceImage(sourceUrl, storageService.store(sourceUrl)));
        } catch (RuntimeException exception) {
            logger.warn(
                    "place_image_download_failed sourceHash={} cause={}",
                    Sha256.hex(sourceUrl),
                    exception.getClass().getSimpleName()
            );
            return java.util.Optional.empty();
        }
    }

    private void deleteStoredFile(String storageKey) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException exception) {
            logger.error(
                    "place_image_cleanup_failed storageKey={} cause={}",
                    storageKey,
                    exception.getClass().getSimpleName()
            );
        }
    }

    private record StoredPlaceImage(String sourceUrl, PlaceImageStorageService.StoredImage image) {
    }
}
