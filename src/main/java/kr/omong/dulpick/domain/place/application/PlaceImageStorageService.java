package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.domain.PlaceImage;
import kr.omong.dulpick.domain.place.domain.PlaceImageRepository;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import kr.omong.dulpick.global.security.crypto.Sha256;

@Service
public class PlaceImageStorageService {

    private final PlaceImageRepository imageRepository;
    private final ContentThumbnailDownloader downloader;
    private final ContentThumbnailProperties properties;
    private final Path storageDirectory;
    private final Clock clock;

    public PlaceImageStorageService(
            PlaceImageRepository imageRepository,
            kr.omong.dulpick.domain.place.infrastructure.KakaoMapImageDownloader downloader,
            ContentThumbnailProperties properties,
            Clock clock
    ) {
        this.imageRepository = imageRepository;
        this.downloader = downloader;
        this.properties = properties;
        this.storageDirectory = Path.of(properties.storagePath()).toAbsolutePath().normalize();
        this.clock = clock;
    }

    public StoredImage store(String sourceUrl) {
        ContentThumbnailDownloader.DownloadedThumbnail downloaded = downloader.download(sourceUrl);
        String storageKey = UUID.randomUUID().toString();
        write(storageKey, downloaded.bytes());
        return new StoredImage(storageKey, downloaded.contentType());
    }

    public StoredImage load(String storageKey) {
        PlaceImage image = imageRepository.findByStorageKey(storageKey)
                .orElseThrow(PublicContentImageUnavailableException::new);
        if (image.getContentType() == null || image.getStorageKey() == null) {
            throw new PublicContentImageUnavailableException();
        }
        try {
            byte[] bytes = Files.readAllBytes(resolve(image.getStorageKey()));
            if (bytes.length == 0 || bytes.length > properties.maxBytes()) {
                throw new PublicContentImageUnavailableException();
            }
            return new StoredImage(storageKey, MediaType.parseMediaType(image.getContentType()), bytes);
        } catch (IOException | IllegalArgumentException exception) {
            throw new PublicContentImageUnavailableException(exception);
        }
    }

    public String publicUrl(String storageKey) {
        return properties.baseUrl() + "/api/v1/place-images/" + storageKey;
    }

    public boolean isStored(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        try {
            Path path = resolve(storageKey);
            long size = Files.size(path);
            return Files.isRegularFile(path) && size > 0 && size <= properties.maxBytes();
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public PlaceImage storeManual(
            Long placeId,
            byte[] bytes,
            MediaType contentType,
            int displayOrder
    ) {
        validateManualImage(bytes, contentType);
        String storageKey = UUID.randomUUID().toString();
        Instant now = clock.instant();
        String sourceUrl = "manual://ops/" + storageKey;
        try {
            write(storageKey, bytes);
            return PlaceImage.createManualStored(
                    placeId,
                    publicUrl(storageKey),
                    Sha256.hex(sourceUrl),
                    storageKey,
                    contentType.toString(),
                    displayOrder,
                    now
            );
        } catch (RuntimeException exception) {
            delete(storageKey);
            throw exception;
        }
    }

    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new PublicContentImageUnavailableException(exception);
        }
    }

    public boolean isPublicUrl(String imageUrl) {
        return imageUrl != null && imageUrl.startsWith(properties.baseUrl() + "/api/v1/place-images/");
    }

    private void validateManualImage(byte[] bytes, MediaType contentType) {
        if (bytes == null || bytes.length == 0 || bytes.length > properties.maxBytes()
                || contentType == null || !"image".equalsIgnoreCase(contentType.getType())
                || !java.util.List.of("jpeg", "png", "webp", "gif")
                .contains(contentType.getSubtype().toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void write(String storageKey, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > properties.maxBytes()) {
            throw new PublicContentImageUnavailableException();
        }
        try {
            Files.createDirectories(storageDirectory);
            Path temporaryFile = storageDirectory.resolve("place-" + storageKey + ".tmp");
            Files.write(temporaryFile, bytes);
            try {
                Files.move(temporaryFile, resolve(storageKey), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new PublicContentImageUnavailableException(exception);
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}")) {
            throw new PublicContentImageUnavailableException();
        }
        Path resolved = storageDirectory.resolve("place-" + storageKey).normalize();
        if (!resolved.startsWith(storageDirectory)) {
            throw new PublicContentImageUnavailableException();
        }
        return resolved;
    }

    public record StoredImage(String storageKey, MediaType contentType, byte[] bytes) {
        public StoredImage(String storageKey, MediaType contentType) {
            this(storageKey, contentType, null);
        }
    }
}
