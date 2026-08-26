package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.application.exception.PlaceImportClaimLostException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PlaceImportResultWriter {

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final Clock clock;
    private final PlaceImportAnalysisWriter analysisWriter;
    private final PlaceImportContentWriter contentWriter;

    public PlaceImportResultWriter(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            Clock clock,
            PlaceImportAnalysisWriter analysisWriter,
            PlaceImportContentWriter contentWriter
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.clock = clock;
        this.analysisWriter = analysisWriter;
        this.contentWriter = contentWriter;
    }

    @Transactional
    public Optional<List<ExtractedPlace>> loadCachedAnalysis(
            Long contentId,
            String contentHash,
            String analyzerModel,
            String promptVersion
    ) {
        return analysisWriter.loadCachedAnalysis(contentId, contentHash, analyzerModel, promptVersion);
    }

    @Transactional
    public String claimAnalysis(
            Long contentId,
            String contentHash,
            String analyzerModel,
            String promptVersion,
            Instant now,
            Instant staleBefore
    ) {
        return analysisWriter.claimAnalysis(contentId, contentHash, analyzerModel, promptVersion, now, staleBefore);
    }

    @Transactional
    public boolean saveAnalysis(
            Long contentId,
            String claimToken,
            String contentHash,
            String analyzerModel,
            String promptVersion,
            List<ExtractedPlace> candidates,
            Instant analyzedAt
    ) {
        return analysisWriter.saveAnalysis(contentId, claimToken, contentHash, analyzerModel,
                promptVersion, candidates, analyzedAt);
    }

    @Transactional
    public void failAnalysis(Long contentId, String claimToken) {
        analysisWriter.failAnalysis(contentId, claimToken);
    }

    @Transactional
    public void saveExtractedCandidates(
            Long importId,
            String claimToken,
            List<ExtractedPlace> candidates
    ) {
        requireClaim(importId, claimToken);
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

    public Long saveMetadata(Long importId, String claimToken, ContentMetadata metadata) {
        return contentWriter.saveMetadata(importId, claimToken, metadata);
    }

    public boolean reuseUnchangedContent(Long importId, String claimToken, ContentMetadata metadata) {
        return contentWriter.reuseUnchangedContent(importId, claimToken, metadata);
    }

    public void saveSuccess(Long importId, String claimToken, ContentMetadata metadata,
                            List<VerifiedCandidate> verifiedCandidates, boolean preserveExistingLinks) {
        contentWriter.saveSuccess(importId, claimToken, metadata, verifiedCandidates, preserveExistingLinks);
    }

    private PlaceImport requireClaim(Long importId, String claimToken) {
        return importRepository.findClaimedForUpdate(importId, claimToken)
                .orElseThrow(PlaceImportClaimLostException::new);
    }

}
