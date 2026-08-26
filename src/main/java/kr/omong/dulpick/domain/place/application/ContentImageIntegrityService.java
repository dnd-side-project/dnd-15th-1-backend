package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
public class ContentImageIntegrityService {

    private static final Logger logger = LoggerFactory.getLogger(ContentImageIntegrityService.class);
    private static final int SCAN_BATCH_SIZE = 200;
    private static final List<String> ACTIVE_BACKLOG_STATUSES = List.of("PENDING", "PROCESSING");

    private final ContentImageRepository imageRepository;
    private final ContentRepository contentRepository;
    private final ContentImageStorageService storageService;
    private final ContentImageEnrichmentBacklogRepository backlogRepository;
    private final Clock clock;

    public ContentImageIntegrityService(
            ContentImageRepository imageRepository,
            ContentRepository contentRepository,
            ContentImageStorageService storageService,
            ContentImageEnrichmentBacklogRepository backlogRepository,
            Clock clock
    ) {
        this.imageRepository = imageRepository;
        this.contentRepository = contentRepository;
        this.storageService = storageService;
        this.backlogRepository = backlogRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 600_000, initialDelay = 120_000)
    public void auditStoredFiles() {
        for (int pageIndex = 0; ; pageIndex++) {
            Page<ContentImage> batch = imageRepository.findByContentTypeIsNotNull(
                    PageRequest.of(pageIndex, SCAN_BATCH_SIZE, Sort.by("imageKey"))
            );
            batch.getContent().forEach(this::audit);
            if (!batch.hasNext()) {
                return;
            }
        }
    }

    void audit(ContentImage image) {
        if (storageService.hasStoredFile(image)) {
            return;
        }
        logger.warn(
                "content_image_file_missing imageKey={} contentId={}",
                image.getImageKey(),
                image.getContentId()
        );
        if (!backlogRepository.existsByContentIdAndStatusIn(image.getContentId(), ACTIVE_BACKLOG_STATUSES)) {
            storageService.registerMissingImageBacklog(image.getContentId());
        }
        repairRepresentativeThumbnail(image.getContentId(), image.getImageKey());
    }

    private void repairRepresentativeThumbnail(Long contentId, String brokenImageKey) {
        Content content = contentRepository.findById(contentId).orElse(null);
        if (content == null || !isBrokenRepresentative(content, brokenImageKey)) {
            return;
        }
        String replacement = imageRepository
                .findAllByContentIdOrderByDisplayOrderAsc(contentId)
                .stream()
                .filter(storageService::hasStoredFile)
                .findFirst()
                .map(image -> storageService.publicUrl(image.getImageKey()))
                .orElse(null);
        content.updateThumbnail(replacement, clock.instant());
        contentRepository.save(content);
    }

    private boolean isBrokenRepresentative(Content content, String brokenImageKey) {
        return content.getThumbnailUrl() != null
                && content.getThumbnailUrl().equals(storageService.publicUrl(brokenImageKey));
    }
}
