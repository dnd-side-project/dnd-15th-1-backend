package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.domain.PlaceImage;
import kr.omong.dulpick.domain.place.domain.PlaceImageRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class PlaceImageStorageService {

    private final PlaceImageRepository imageRepository;
    private final ContentThumbnailDownloader downloader;
    private final ContentThumbnailProperties properties;
    private final Path storageDirectory;

    public PlaceImageStorageService(
            PlaceImageRepository imageRepository,
            kr.omong.dulpick.domain.place.infrastructure.KakaoMapImageDownloader downloader,
            ContentThumbnailProperties properties
    ) {
        this.imageRepository = imageRepository;
        this.downloader = downloader;
        this.properties = properties;
        this.storageDirectory = Path.of(properties.storagePath()).toAbsolutePath().normalize();
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

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new PublicContentImageUnavailableException(exception);
        }
    }

    public boolean isPublicUrl(String imageUrl) {
        return imageUrl != null && imageUrl.startsWith(properties.baseUrl() + "/api/v1/place-images/");
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
