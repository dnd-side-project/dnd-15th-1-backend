package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.infrastructure.PublicInstagramMetadataProvider;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ContentImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ContentImageStorageService.class);
    private static final int MAX_IMAGE_URL_ATTEMPTS = 4;

    private final ContentImageRepository imageRepository;
    private final ContentRepository contentRepository;
    private final ContentThumbnailDownloader downloader;
    private final PublicInstagramMetadataProvider metadataProvider;
    private final ContentThumbnailProperties properties;
    private final Clock clock;
    private final Path storageDirectory;

    public ContentImageStorageService(
            ContentImageRepository imageRepository,
            ContentRepository contentRepository,
            @Qualifier("instagramThumbnailDownloader") ContentThumbnailDownloader downloader,
            PublicInstagramMetadataProvider metadataProvider,
            ContentThumbnailProperties properties,
            Clock clock
    ) {
        this.imageRepository = imageRepository;
        this.contentRepository = contentRepository;
        this.downloader = downloader;
        this.metadataProvider = metadataProvider;
        this.properties = properties;
        this.clock = clock;
        this.storageDirectory = Path.of(properties.storagePath()).toAbsolutePath().normalize();
    }

    @Transactional
    public void storeIfAvailable(Long contentId, List<String> sourceUrls) {
        if (contentId == null) {
            return;
        }
        contentRepository.findById(contentId)
                .ifPresent(content -> storeIfAvailable(content, sourceUrls));
    }

    @Transactional
    public void storeIfAvailable(Content content, List<String> sourceUrls) {
        if (content == null || !isInstagramContent(content) || sourceUrls == null) {
            return;
        }
        List<String> imageUrls = normalizeImageUrls(sourceUrls);
        if (imageUrls.isEmpty()) {
            return;
        }
        Map<String, ContentImage> existingImages = imageRepository
                .findAllByContentIdOrderByDisplayOrderAsc(content.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ContentImage::getSourceUrlHash,
                        image -> image,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        Set<String> occupiedHashes = new HashSet<>(existingImages.keySet());
        Instant now = clock.instant();
        List<ContentImage> images = new ArrayList<>();
        List<ContentImage> failedImages = new ArrayList<>();
        for (int index = 0; index < imageUrls.size(); index++) {
            ContentImage image = saveImage(
                    content.getId(), imageUrls, existingImages, occupiedHashes, index, now
            );
            existingImages.put(image.getSourceUrlHash(), image);
            if (hasStoredFile(image) && !images.contains(image)) {
                images.add(image);
            } else if (!hasStoredFile(image) && !failedImages.contains(image)) {
                failedImages.add(image);
            }
        }
        retryNewImagesFromOriginalContent(content, failedImages, occupiedHashes, images);
        imageRepository.saveAll(images);
    }

    @Transactional
    public void refreshExistingIfAvailable(Content content, List<String> sourceUrls) {
        if (content == null || !isInstagramContent(content) || sourceUrls == null) {
            return;
        }
        List<String> imageUrls = normalizeImageUrls(sourceUrls);
        if (imageUrls.isEmpty()) {
            return;
        }
        List<ContentImage> existingImages = imageRepository
                .findAllByContentIdOrderByDisplayOrderAsc(content.getId());
        if (existingImages.isEmpty()) {
            storeIfAvailable(content, imageUrls);
            return;
        }
        Set<String> occupiedHashes = existingImages.stream()
                .map(ContentImage::getSourceUrlHash)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<ContentImage> failedImages = new ArrayList<>();
        for (ContentImage image : existingImages) {
            if (hasStoredFile(image)) {
                continue;
            }
            if (!storeFromCandidates(image, imageUrls, occupiedHashes)) {
                failedImages.add(image);
                logger.warn(
                        "Content image refresh failed: contentId={}, imageKey={}, cause={}",
                        content.getId(),
                        image.getImageKey(),
                        "ALL_CANDIDATES_REJECTED"
                );
            }
        }
        retryFailedImages(content, failedImages, occupiedHashes);
        imageRepository.saveAllAndFlush(existingImages);
    }

    @Transactional
    public StoredImage load(String imageKey) {
        ContentImage image = imageRepository.findById(imageKey)
                .orElseThrow(PublicContentImageUnavailableException::new);
        Content content = contentRepository.findByIdAndPublicationStatus(
                        image.getContentId(), ContentPublicationStatus.PUBLIC
                )
                .orElseThrow(PublicContentImageUnavailableException::new);
        try {
            if (hasStoredFile(image)) {
                return read(image);
            }
            return downloadAndStore(image, image.getSourceUrl());
        } catch (IOException | RuntimeException exception) {
            return refreshFromOriginalContent(content, image, exception);
        }
    }

    public boolean hasAllStoredImages(Long contentId) {
        if (contentId == null) {
            return false;
        }
        List<ContentImage> images = imageRepository.findAllByContentIdOrderByDisplayOrderAsc(contentId);
        return !images.isEmpty() && images.stream().allMatch(this::hasStoredFile);
    }

    private void retryNewImagesFromOriginalContent(
            Content content,
            List<ContentImage> failedImages,
            Set<String> occupiedHashes,
            List<ContentImage> storedImages
    ) {
        if (failedImages.isEmpty()) {
            return;
        }
        List<String> freshUrls;
        try {
            freshUrls = normalizeImageUrls(metadataProvider.fetchImageUrls(content.getCanonicalUrl()));
        } catch (RuntimeException exception) {
            logger.warn(
                    "Content image original refresh failed: contentId={}, cause={}",
                    content.getId(),
                    exception.getClass().getSimpleName()
            );
            return;
        }
        for (ContentImage image : failedImages) {
            if (storeFromCandidates(image, freshUrls, occupiedHashes)
                    && !storedImages.contains(image)) {
                storedImages.add(image);
            }
        }
    }

    private void retryFailedImages(
            Content content,
            List<ContentImage> failedImages,
            Set<String> occupiedHashes
    ) {
        if (failedImages.isEmpty()) {
            return;
        }
        List<String> retryUrls;
        try {
            retryUrls = metadataProvider.fetchImageUrls(content.getCanonicalUrl());
        } catch (RuntimeException exception) {
            return;
        }
        for (ContentImage image : failedImages) {
            if (!storeFromCandidates(image, retryUrls, occupiedHashes)) {
                logger.warn(
                        "Content image retry failed: contentId={}, imageKey={}, cause={}",
                        content.getId(),
                        image.getImageKey(),
                        "ALL_CANDIDATES_REJECTED"
                );
            }
        }
    }

    private StoredImage refreshFromOriginalContent(
            Content content,
            ContentImage image,
            Throwable originalFailure
    ) {
        if (!isInstagramContent(content)) {
            throw new PublicContentImageUnavailableException(originalFailure);
        }
        RuntimeException lastFailure = originalFailure instanceof RuntimeException runtimeException
                ? runtimeException
                : new PublicContentImageUnavailableException(originalFailure);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                List<String> freshUrls = metadataProvider.fetchImageUrls(content.getCanonicalUrl());
                StoredImage refreshed = downloadFromCandidates(image, freshUrls);
                if (refreshed != null) {
                    return refreshed;
                }
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw new PublicContentImageUnavailableException(lastFailure);
    }

    private boolean storeFromCandidates(
            ContentImage image,
            List<String> freshUrls,
            Set<String> occupiedHashes
    ) {
        String previousHash = image.getSourceUrlHash();
        List<String> candidates = orderCandidates(freshUrls, image.getDisplayOrder()).stream()
                .filter(url -> {
                    String hash = Sha256.hex(url);
                    return !occupiedHashes.contains(hash) || hash.equals(previousHash);
                })
                .toList();
        for (int index = 0; index < Math.min(candidates.size(), MAX_IMAGE_URL_ATTEMPTS); index++) {
            String freshUrl = candidates.get(index);
            try {
                downloadAndStore(image, freshUrl);
                occupiedHashes.remove(previousHash);
                occupiedHashes.add(image.getSourceUrlHash());
                return true;
            } catch (RuntimeException ignored) {
                // Try another fresh image URL for the same stable image key.
            }
        }
        return false;
    }

    private StoredImage downloadFromCandidates(ContentImage image, List<String> freshUrls) {
        List<String> candidates = orderCandidates(freshUrls, image.getDisplayOrder());
        for (int index = 0; index < Math.min(candidates.size(), MAX_IMAGE_URL_ATTEMPTS); index++) {
            String freshUrl = candidates.get(index);
            try {
                return downloadAndStore(image, freshUrl);
            } catch (RuntimeException ignored) {
                // Try another fresh image URL for the same stable image key.
            }
        }
        return null;
    }

    private List<String> orderCandidates(List<String> freshUrls, int displayOrder) {
        List<String> availableUrls = freshUrls == null
                ? List.of()
                : freshUrls.stream().filter(url -> url != null && !url.isBlank()).distinct().toList();
        if (availableUrls.isEmpty()) {
            return List.of();
        }
        int preferredIndex = Math.min(Math.max(displayOrder, 0), availableUrls.size() - 1);
        List<String> orderedUrls = new ArrayList<>();
        orderedUrls.add(availableUrls.get(preferredIndex));
        for (int index = 0; index < availableUrls.size(); index++) {
            if (index != preferredIndex) {
                orderedUrls.add(availableUrls.get(index));
            }
        }
        return orderedUrls;
    }

    private StoredImage downloadAndStore(ContentImage image, String sourceUrl) {
        ContentThumbnailDownloader.DownloadedThumbnail downloaded = downloader.download(sourceUrl);
        write(image.getStorageKey(), downloaded.bytes());
        Instant now = clock.instant();
        image.replaceSource(sourceUrl, Sha256.hex(sourceUrl), now);
        image.markStored(downloaded.contentType().toString(), now);
        return new StoredImage(downloaded.bytes(), downloaded.contentType());
    }

    private ContentImage saveImage(
            Long contentId,
            List<String> sourceUrls,
            Map<String, ContentImage> existingImages,
            Set<String> occupiedHashes,
            int displayOrder,
            Instant now
    ) {
        String sourceUrl = sourceUrls.get(displayOrder);
        String sourceUrlHash = Sha256.hex(sourceUrl);
        ContentImage image = existingImages.get(sourceUrlHash);
        if (image == null) {
            image = ContentImage.create(contentId, sourceUrl, sourceUrlHash, displayOrder, now);
        } else {
            image.updateDisplayOrder(displayOrder, now);
        }
        occupiedHashes.add(image.getSourceUrlHash());
        if (hasStoredFile(image)) {
            return image;
        }
        try {
            if (!storeFromCandidates(image, sourceUrls, occupiedHashes)) {
                throw new PublicContentImageUnavailableException();
            }
        } catch (RuntimeException exception) {
            logger.warn(
                    "Content image storage failed: contentId={}, imageKey={}, cause={}",
                    contentId,
                    image.getImageKey(),
                    exception.getClass().getSimpleName()
            );
        }
        return image;
    }

    private List<String> normalizeImageUrls(List<String> sourceUrls) {
        if (sourceUrls == null) {
            return List.of();
        }
        return sourceUrls.stream()
                .map(this::normalizeImageUrl)
                .filter(url -> url != null && !url.isBlank() && url.length() <= 2_000)
                .distinct()
                .limit(properties.maxImages())
                .toList();
    }

    private String normalizeImageUrl(String sourceUrl) {
        if (sourceUrl == null) {
            return null;
        }
        return org.springframework.web.util.HtmlUtils.htmlUnescape(sourceUrl)
                .replace("\\u002F", "/")
                .replace("\\u0026", "&")
                .replace("\\u003D", "=")
                .replace("\\/", "/")
                .strip();
    }

    private boolean isInstagramContent(Content content) {
        return content.getSourceType() == ContentSourceType.INSTAGRAM_REEL
                || content.getSourceType() == ContentSourceType.INSTAGRAM_POST;
    }

    private boolean hasStoredFile(ContentImage image) {
        return image.getContentType() != null
                && !image.getContentType().isBlank()
                && Files.isRegularFile(resolve(image.getStorageKey()))
                && fileSize(image.getStorageKey()) <= properties.maxBytes();
    }

    private StoredImage read(ContentImage image) throws IOException {
        return new StoredImage(
                Files.readAllBytes(resolve(image.getStorageKey())),
                MediaType.parseMediaType(image.getContentType())
        );
    }

    private void write(String storageKey, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > properties.maxBytes()) {
            throw new PublicContentImageUnavailableException();
        }
        try {
            Files.createDirectories(storageDirectory);
            Path temporaryFile = storageDirectory.resolve(storageKey + "." + UUID.randomUUID() + ".tmp");
            Files.write(temporaryFile, bytes);
            try {
                Files.move(
                        temporaryFile,
                        resolve(storageKey),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new PublicContentImageUnavailableException(exception);
        }
    }

    private long fileSize(String storageKey) {
        try {
            return Files.size(resolve(storageKey));
        } catch (IOException exception) {
            return Long.MAX_VALUE;
        }
    }

    private Path resolve(String storageKey) {
        Path resolved = storageDirectory.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageDirectory)) {
            throw new PublicContentImageUnavailableException();
        }
        return resolved;
    }

    public record StoredImage(
            byte[] bytes,
            MediaType contentType
    ) {
    }

}
