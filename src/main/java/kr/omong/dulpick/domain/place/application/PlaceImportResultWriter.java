package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlace;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSubmission;
import kr.omong.dulpick.domain.place.domain.ContentSubmissionRepository;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PlaceImportResultWriter {

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final ContentRepository contentRepository;
    private final ContentPlaceRepository contentPlaceRepository;
    private final ContentSubmissionRepository submissionRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public PlaceImportResultWriter(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            ContentRepository contentRepository,
            ContentPlaceRepository contentPlaceRepository,
            ContentSubmissionRepository submissionRepository,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.contentRepository = contentRepository;
        this.contentPlaceRepository = contentPlaceRepository;
        this.submissionRepository = submissionRepository;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<List<ExtractedPlace>> loadCachedAnalysis(
            Long contentId,
            String contentHash,
            String analyzerModel,
            String promptVersion
    ) {
        return contentRepository.findById(contentId)
                .filter(content -> contentHash.equals(content.getAnalysisContentHash()))
                .filter(content -> analyzerModel.equals(content.getAnalyzerModel()))
                .filter(content -> promptVersion.equals(content.getPromptVersion()))
                .filter(content -> content.getAnalyzedAt() != null)
                .flatMap(this::readCandidates);
    }

    @Transactional
    public boolean claimAnalysis(
            Long contentId,
            Instant now,
            Instant staleBefore
    ) {
        return contentRepository.claimAnalysis(contentId, now, staleBefore) == 1;
    }

    @Transactional
    public void saveAnalysis(
            Long contentId,
            String contentHash,
            String analyzerModel,
            String promptVersion,
            List<ExtractedPlace> candidates,
            Instant analyzedAt
    ) {
        contentRepository.findById(contentId).ifPresent(content -> {
            try {
                content.updateExtractedAnalysis(
                        contentHash,
                        analyzerModel,
                        promptVersion,
                        objectMapper.writeValueAsString(candidates),
                        analyzedAt
                );
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to cache place analysis", exception);
            }
        });
    }

    @Transactional
    public void failAnalysis(Long contentId) {
        contentRepository.failAnalysis(contentId);
    }

    @Transactional
    public void saveExtractedCandidates(Long importId, List<ExtractedPlace> candidates) {
        candidateRepository.deleteAllByImportId(importId);
        candidateRepository.saveAll(candidates.stream()
                .map(candidate -> PlaceCandidate.extracted(
                        importId,
                        candidate.name(),
                        candidate.addressHint(),
                        candidate.evidence(),
                        candidate.mentionType(),
                        clock.instant()
                ))
                .toList());
    }

    private Optional<List<ExtractedPlace>> readCandidates(Content content) {
        try {
            ExtractedPlace[] candidates = objectMapper.readValue(
                    content.getExtractedCandidatesJson(),
                    ExtractedPlace[].class
            );
            return Optional.of(List.of(candidates));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    @Transactional
    public Long saveMetadata(Long importId, ContentMetadata metadata) {
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(IllegalStateException::new);
        Content content = findOrCreateContent(metadata);
        placeImport.attachContent(content.getId());
        submissionRepository.insertIfAbsent(content.getId(), placeImport.getMemberId(), clock.instant());
        placeImport.recordMetadata(
                displayTitle(metadata),
                metadata.caption(),
                metadata.thumbnailUrl(),
                metadata.contentHash(),
                metadata.sourceUpdatedAt()
        );
        return content.getId();
    }

    @Transactional
    public boolean reuseUnchangedContent(Long importId, ContentMetadata metadata) {
        if (!metadata.sourceType().storesPublicContent()) {
            return false;
        }
        String urlHash = Sha256.hex(metadata.canonicalUrl());
        Content content = contentRepository.findByCanonicalUrlHash(urlHash).orElse(null);
        if (content == null || !metadata.contentHash().equals(content.getContentHash())) {
            return false;
        }
        List<Place> places = contentPlaceRepository.findAllByContentId(content.getId()).stream()
                .map(ContentPlace::getPlaceId)
                .map(placeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .toList();
        if (places.isEmpty()) {
            return false;
        }
        saveMetadata(importId, metadata);
        candidateRepository.deleteAllByImportId(importId);
        candidateRepository.saveAll(places.stream()
                .map(place -> PlaceCandidate.verified(
                        importId,
                        place.getId(),
                        place.getName(),
                        place.getAddress(),
                        null,
                        null,
                        clock.instant()
                ))
                .toList());
        importRepository.findById(importId).orElseThrow(IllegalStateException::new)
                .complete(
                        displayTitle(metadata),
                        metadata.caption(),
                        metadata.thumbnailUrl(),
                        metadata.contentHash(),
                        metadata.sourceUpdatedAt(),
                        clock.instant()
                );
        return true;
    }

    @Transactional
    public void saveSuccess(
            Long importId,
            ContentMetadata metadata,
            List<VerifiedCandidate> verifiedCandidates
    ) {
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(IllegalStateException::new);
        Long contentId = placeImport.getContentId();
        if (contentId == null && metadata.sourceType().storesPublicContent()) {
            contentId = findOrCreateContent(metadata).getId();
            placeImport.attachContent(contentId);
        }
        final Long resolvedContentId = contentId;
        candidateRepository.deleteAllByImportId(importId);
        if (resolvedContentId != null) {
            contentPlaceRepository.deleteAllByContentId(resolvedContentId);
        }
        List<PlaceCandidate> candidates = verifiedCandidates.stream()
                .map(candidate -> saveCandidate(importId, resolvedContentId, candidate))
                .toList();
        candidateRepository.saveAll(candidates);
        if (resolvedContentId != null) {
            contentRepository.findById(resolvedContentId).ifPresent(content -> content.updateMetadata(
                    displayTitle(metadata),
                    metadata.caption(),
                    metadata.thumbnailUrl(),
                    metadata.contentHash(),
                    clock.instant()
            ));
            contentRepository.findById(resolvedContentId).ifPresent(content -> content.updateSourceMetadata(
                    metadata.sourceAuthorName(),
                    metadata.sourceAuthorUsername(),
                    metadata.sourcePublishedOn(),
                    metadata.likeCount(),
                    metadata.commentCount(),
                    metadata.engagementCheckedAt()
            ));
            candidates.forEach(candidate -> {
                Long placeId = candidate.getPlaceId();
                contentPlaceRepository.insertIfAbsent(resolvedContentId, placeId, clock.instant());
            });
            contentRepository.findById(resolvedContentId)
                    .ifPresent(content -> content.updatePlaceCount(candidates.size()));
        }
        if (resolvedContentId != null) {
            contentRepository.findById(resolvedContentId).ifPresent(content -> content.publish(clock.instant()));
        }
        placeImport.complete(
                displayTitle(metadata),
                metadata.caption(),
                metadata.thumbnailUrl(),
                metadata.contentHash(),
                metadata.sourceUpdatedAt(),
                clock.instant()
        );
    }

    private PlaceCandidate saveCandidate(
            Long importId,
            Long contentId,
            VerifiedCandidate candidate
    ) {
        VerifiedPlace verified = candidate.verified();
        placeRepository.insertIfAbsent(
                verified.kakaoPlaceId(),
                verified.name(),
                verified.address(),
                verified.roadAddress(),
                verified.latitude(),
                verified.longitude(),
                verified.category(),
                verified.thumbnailUrl(),
                clock.instant()
        );
        Place place = placeRepository.findByKakaoPlaceId(verified.kakaoPlaceId())
                .orElseThrow(IllegalStateException::new);
        return PlaceCandidate.verified(
                importId,
                place.getId(),
                candidate.extracted().name(),
                candidate.extracted().addressHint(),
                candidate.extracted().evidence(),
                candidate.extracted().mentionType(),
                clock.instant()
        );
    }

    private Content findOrCreateContent(ContentMetadata metadata) {
        String urlHash = Sha256.hex(metadata.canonicalUrl());
        contentRepository.insertIfAbsent(
                metadata.canonicalUrl(),
                urlHash,
                metadata.sourceType().name(),
                displayTitle(metadata),
                metadata.caption(),
                metadata.thumbnailUrl(),
                metadata.contentHash(),
                clock.instant()
        );
        Content content = contentRepository.findByCanonicalUrlHash(urlHash)
                .orElseThrow(IllegalStateException::new);
        return content;
    }

    private String fitTitle(String title) {
        if (title == null || title.length() <= 4_000) {
            return title;
        }
        return title.substring(0, 4_000);
    }

    private String displayTitle(ContentMetadata metadata) {
        String title = metadata.title() == null ? "" : metadata.title().strip();
        if (metadata.sourceType().name().startsWith("INSTAGRAM")) {
            int separator = title.indexOf(": ");
            if (separator >= 0 && separator + 2 < title.length()) {
                title = title.substring(separator + 2).strip();
            }
        }
        String firstLine = title.split("\\R", 2)[0].strip();
        if (firstLine.isBlank()) {
            firstLine = (metadata.caption() == null ? "" : metadata.caption()).strip();
        }
        firstLine = firstLine.replaceAll("^[\\\"'‘’“”]+|[\\\"'‘’“”]+$", "").strip();
        return fitTitle(firstLine.length() > 200 ? firstLine.substring(0, 200).strip() : firstLine);
    }
}
