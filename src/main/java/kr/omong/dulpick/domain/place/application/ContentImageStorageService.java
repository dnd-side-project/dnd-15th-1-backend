package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.infrastructure.PublicInstagramMetadataProvider;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.RejectedExecutionException;

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
    private final Executor refreshExecutor;
    private final ContentImageEnrichmentBacklogRepository backlogRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private static final Duration MIN_CONTENT_REFRESH_INTERVAL = Duration.ofSeconds(60);
    private final Set<Long> refreshInFlightByContent = ConcurrentHashMap.newKeySet();
    private final Map<Long, Instant> lastContentRefreshAt = new ConcurrentHashMap<>();

    @Autowired
    public ContentImageStorageService(
            ContentImageRepository imageRepository,
            ContentRepository contentRepository,
            @Qualifier("instagramThumbnailDownloader") ContentThumbnailDownloader downloader,
            PublicInstagramMetadataProvider metadataProvider,
            ContentThumbnailProperties properties,
            Clock clock,
            PlatformTransactionManager transactionManager,
            @Qualifier("contentImageExecutor") Executor refreshExecutor,
            ContentImageEnrichmentBacklogRepository backlogRepository,
            ObjectMapper objectMapper
    ) {
        this.imageRepository = imageRepository;
        this.contentRepository = contentRepository;
        this.downloader = downloader;
        this.metadataProvider = metadataProvider;
        this.properties = properties;
        this.clock = clock;
        this.storageDirectory = Path.of(properties.storagePath()).toAbsolutePath().normalize();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.refreshExecutor = refreshExecutor;
        this.backlogRepository = backlogRepository;
        this.objectMapper = objectMapper;
    }

    public ContentImageStorageService(
            ContentImageRepository imageRepository,
            ContentRepository contentRepository,
            @Qualifier("instagramThumbnailDownloader") ContentThumbnailDownloader downloader,
            PublicInstagramMetadataProvider metadataProvider,
            ContentThumbnailProperties properties,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.imageRepository = imageRepository;
        this.contentRepository = contentRepository;
        this.downloader = downloader;
        this.metadataProvider = metadataProvider;
        this.properties = properties;
        this.clock = clock;
        this.storageDirectory = Path.of(properties.storagePath()).toAbsolutePath().normalize();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.refreshExecutor = null;
        this.backlogRepository = null;
        this.objectMapper = null;
    }

    public void storeIfAvailable(Long contentId, List<String> sourceUrls) {
        if (contentId == null) {
            return;
        }
        contentRepository.findById(contentId)
                .ifPresent(content -> storeIfAvailable(content, sourceUrls));
    }

    public void storeIfAvailable(Content content, List<String> sourceUrls) {
        if (content == null || !isInstagramContent(content) || sourceUrls == null) {
            return;
        }
        List<String> imageUrls = normalizeImageUrls(sourceUrls);
        if (imageUrls.isEmpty()) {
            return;
        }
        List<ContentImage> storedImages = imageRepository.findAllByContentIdOrderByDisplayOrderAsc(content.getId());
        Set<String> existingImageKeys = storedImages.stream()
                .map(ContentImage::getImageKey)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, ContentImage> existingImages = storedImages
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
            if (!images.contains(image)) {
                images.add(image);
            }
            if (!hasStoredFile(image) && !failedImages.contains(image)) {
                failedImages.add(image);
            }
        }
        retryNewImagesFromOriginalContent(content, failedImages, occupiedHashes, images);
        DuplicateRemoval removal = deduplicateStoredImages(images, existingImageKeys);
        transactionTemplate.executeWithoutResult(status -> persistImages(removal));
    }

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
        Set<String> existingImageKeys = existingImages.stream()
                .map(ContentImage::getImageKey)
                .collect(java.util.stream.Collectors.toSet());
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
        DuplicateRemoval removal = deduplicateStoredImages(existingImages, existingImageKeys);
        transactionTemplate.executeWithoutResult(status -> persistImages(removal));
    }

    public StoredImage load(String imageKey) {
        ContentImage image = imageRepository.findById(imageKey)
                .orElseThrow(PublicContentImageUnavailableException::new);
        contentRepository.findByIdAndPublicationStatus(
                        image.getContentId(), ContentPublicationStatus.PUBLIC
                )
                .orElseThrow(PublicContentImageUnavailableException::new);
        if (!hasStoredFile(image)) {
            dispatchRefresh(image);
            throw new PublicContentImageUnavailableException();
        }
        try {
            return read(image);
        } catch (IOException exception) {
            dispatchRefresh(image);
            throw new PublicContentImageUnavailableException(exception);
        }
    }

    public boolean hasAllStoredImages(Long contentId) {
        if (contentId == null) {
            return false;
        }
        List<ContentImage> images = imageRepository.findAllByContentIdOrderByDisplayOrderAsc(contentId);
        return !images.isEmpty() && images.stream().allMatch(this::hasStoredFile);
    }

    @Transactional
    public ContentImage storeManual(
            Long contentId,
            byte[] bytes,
            MediaType contentType,
            boolean makeThumbnail
    ) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(PublicContentImageUnavailableException::new);
        validateManualImage(bytes, contentType);
        List<ContentImage> images = imageRepository.findAllByContentIdOrderByDisplayOrderAsc(contentId);
        ContentImage image = images.stream()
                .filter(candidate -> candidate.getContentType() == null
                        || candidate.getContentType().isBlank())
                .findFirst()
                .orElse(null);
        if (image == null && images.size() >= properties.maxImages()) {
            throw new PublicContentImageUnavailableException();
        }
        Instant now = clock.instant();
        int displayOrder = makeThumbnail
                ? 0
                : image == null ? images.size() : image.getDisplayOrder();
        if (makeThumbnail) {
            for (ContentImage candidate : images) {
                if (candidate != image) {
                    candidate.updateDisplayOrder(candidate.getDisplayOrder() + 1, now);
                }
            }
            displayOrder = 0;
        }
        String imageKey = image == null ? UUID.randomUUID().toString() : image.getImageKey();
        String sourceUrl = "manual://ops/" + imageKey;
        if (image == null) {
            image = ContentImage.createManual(
                    content.getId(),
                    sourceUrl,
                    Sha256.hex(sourceUrl),
                    displayOrder,
                    now
            );
        } else {
            image.replaceSource(sourceUrl, Sha256.hex(sourceUrl), now);
            image.updateDisplayOrder(displayOrder, now);
        }
        try {
            write(image.getStorageKey(), bytes);
            image.markStored(contentType.toString(), Sha256.hex(bytes), now);
            ContentImage saved = imageRepository.saveAndFlush(image);
            if (makeThumbnail || content.getThumbnailUrl() == null) {
                content.updateThumbnail(publicUrl(saved.getImageKey()), now);
            }
            return saved;
        } catch (RuntimeException exception) {
            deleteStoredFile(image.getStorageKey());
            throw exception;
        }
    }

    @Transactional
    public void deleteManual(String imageKey, Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PUBLIC_CONTENT_NOT_FOUND));
        ContentImage image = imageRepository.findById(imageKey)
                .filter(candidate -> candidate.getContentId().equals(contentId))
                .orElseThrow(PublicContentImageUnavailableException::new);
        deleteStoredFile(image.getStorageKey());
        imageRepository.delete(image);
        imageRepository.flush();
        List<ContentImage> remaining = imageRepository.findAllByContentIdOrderByDisplayOrderAsc(contentId);
        for (int index = 0; index < remaining.size(); index++) {
            remaining.get(index).updateDisplayOrder(index, clock.instant());
        }
        if (content.getThumbnailUrl() != null
                && content.getThumbnailUrl().equals(publicUrl(image.getImageKey()))) {
            content.updateThumbnail(
                    remaining.isEmpty() ? null : publicUrl(remaining.getFirst().getImageKey()),
                    clock.instant()
            );
        }
    }

    public String publicUrl(String imageKey) {
        return properties.baseUrl() + "/api/v1/content-images/" + imageKey;
    }

    public String adminUrl(Long contentId, String imageKey) {
        return properties.baseUrl() + "/api/v1/admin/contents/" + contentId
                + "/images/" + imageKey + "/file";
    }

    @Transactional(readOnly = true)
    public StoredImage loadForAdmin(String imageKey, Long contentId) {
        ContentImage image = imageRepository.findById(imageKey)
                .filter(candidate -> candidate.getContentId().equals(contentId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!hasStoredFile(image)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        try {
            return read(image);
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private void validateManualImage(byte[] bytes, MediaType contentType) {
        if (bytes == null || bytes.length == 0 || bytes.length > properties.maxBytes()
                || contentType == null || !"image".equalsIgnoreCase(contentType.getType())
                || !List.of("jpeg", "png", "webp", "gif").contains(contentType.getSubtype().toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
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
        image.markStored(
                downloaded.contentType().toString(),
                Sha256.hex(downloaded.bytes()),
                now
        );
        return new StoredImage(downloaded.bytes(), downloaded.contentType());
    }

    private void dispatchRefresh(ContentImage image) {
        Long contentId = image.getContentId();
        if (contentId == null) {
            return;
        }
        if (refreshExecutor == null) {
            registerMissingImageBacklog(contentId);
            return;
        }
        if (!canDispatchRefresh(contentId)) {
            return;
        }
        if (!refreshInFlightByContent.add(contentId)) {
            return;
        }
        try {
            refreshExecutor.execute(() -> {
                try {
                    refreshContentImages(contentId);
                } catch (RuntimeException exception) {
                    logger.warn(
                            "Content image background refresh failed: contentId={}, cause={}",
                            contentId,
                            exception.getClass().getSimpleName()
                    );
                    registerMissingImageBacklog(contentId);
                } finally {
                    lastContentRefreshAt.put(contentId, clock.instant());
                    refreshInFlightByContent.remove(contentId);
                }
            });
        } catch (RejectedExecutionException exception) {
            refreshInFlightByContent.remove(contentId);
            logger.warn(
                    "Content image background refresh rejected: contentId={}, cause={}",
                    contentId,
                    exception.getClass().getSimpleName()
            );
            registerMissingImageBacklog(contentId);
        }
    }

    private boolean canDispatchRefresh(Long contentId) {
        Instant lastAttemptAt = lastContentRefreshAt.get(contentId);
        if (lastAttemptAt == null) {
            return true;
        }
        return lastAttemptAt.plus(MIN_CONTENT_REFRESH_INTERVAL).isBefore(clock.instant());
    }

    public void registerMissingImageBacklog(Long contentId) {
        if (backlogRepository == null || objectMapper == null || contentId == null) {
            return;
        }
        try {
            List<String> sourceUrls = imageRepository
                    .findAllByContentIdOrderByDisplayOrderAsc(contentId)
                    .stream()
                    .map(ContentImage::getSourceUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
            if (sourceUrls.isEmpty()) {
                return;
            }
            Instant now = clock.instant();
            backlogRepository.enqueue(
                    contentId,
                    objectMapper.writeValueAsString(sourceUrls),
                    now.plusSeconds(60),
                    now
            );
        } catch (RuntimeException exception) {
            logger.warn(
                    "Content image backlog registration failed: contentId={}, cause={}",
                    contentId,
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void refreshContentImages(Long contentId) {
        Content content = contentRepository.findByIdAndPublicationStatus(
                        contentId, ContentPublicationStatus.PUBLIC
                )
                .orElse(null);
        if (content == null || !isInstagramContent(content)) {
            return;
        }
        List<ContentImage> images = imageRepository.findAllByContentIdOrderByDisplayOrderAsc(contentId);
        List<ContentImage> brokenImages = images.stream()
                .filter(image -> !hasStoredFile(image))
                .toList();
        if (brokenImages.isEmpty()) {
            return;
        }
        List<String> freshUrls = normalizeImageUrls(fetchFreshImageUrls(content));
        if (freshUrls.isEmpty()) {
            registerMissingImageBacklog(contentId);
            return;
        }
        Set<String> occupiedHashes = images.stream()
                .map(ContentImage::getSourceUrlHash)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        for (ContentImage image : brokenImages) {
            if (!storeFromCandidates(image, freshUrls, occupiedHashes)) {
                logger.warn(
                        "Content image background retry failed: contentId={}, imageKey={}, cause={}",
                        contentId,
                        image.getImageKey(),
                        "ALL_CANDIDATES_REJECTED"
                );
            }
        }
        transactionTemplate.executeWithoutResult(status -> imageRepository.saveAll(brokenImages));
    }

    private List<String> fetchFreshImageUrls(Content content) {
        try {
            return metadataProvider.fetchImageUrls(content.getCanonicalUrl());
        } catch (RuntimeException exception) {
            logger.warn(
                    "Content image metadata refresh failed: contentId={}, cause={}",
                    content.getId(),
                    exception.getClass().getSimpleName()
            );
            return List.of();
        }
    }

    private DuplicateRemoval deduplicateStoredImages(
            List<ContentImage> images,
            Set<String> existingImageKeys
    ) {
        Map<String, ContentImage> uniqueImages = new LinkedHashMap<>();
        List<ContentImage> duplicates = new ArrayList<>();
        for (ContentImage image : images) {
            String contentHash = storedContentHash(image);
            if (contentHash == null) {
                continue;
            }
            ContentImage previous = uniqueImages.putIfAbsent(contentHash, image);
            if (previous != null && previous != image) {
                duplicates.add(image);
                deleteStoredFile(image.getStorageKey());
            }
        }
        List<String> persistedDuplicateKeys = duplicates.stream()
                .map(ContentImage::getImageKey)
                .filter(existingImageKeys::contains)
                .toList();
        List<ContentImage> survivors = images.stream()
                .filter(image -> !duplicates.contains(image))
                .toList();
        return new DuplicateRemoval(survivors, persistedDuplicateKeys);
    }

    private void persistImages(DuplicateRemoval removal) {
        if (!removal.removedKeys().isEmpty()) {
            imageRepository.deleteAllById(removal.removedKeys());
        }
        imageRepository.saveAll(removal.survivors());
    }

    private String storedContentHash(ContentImage image) {
        if (!hasStoredFile(image)) {
            return null;
        }
        if (image.getContentHash() != null && !image.getContentHash().isBlank()) {
            return image.getContentHash();
        }
        try {
            String contentHash = Sha256.hex(Files.readAllBytes(resolve(image.getStorageKey())));
            image.markContentHash(contentHash, clock.instant());
            return contentHash;
        } catch (IOException exception) {
            return null;
        }
    }

    private void deleteStoredFile(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            logger.warn(
                    "Content image duplicate cleanup failed: storageKey={}, cause={}",
                    storageKey,
                    exception.getClass().getSimpleName()
            );
        }
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

    public boolean hasStoredFile(ContentImage image) {
        if (image == null || image.getContentType() == null || image.getContentType().isBlank()) {
            return false;
        }
        try {
            Path path = resolve(image.getStorageKey());
            long size = Files.size(path);
            return Files.isRegularFile(path) && size > 0 && size <= properties.maxBytes();
        } catch (IOException | RuntimeException exception) {
            return false;
        }
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

    private record DuplicateRemoval(
            List<ContentImage> survivors,
            List<String> removedKeys
    ) {
    }

}
