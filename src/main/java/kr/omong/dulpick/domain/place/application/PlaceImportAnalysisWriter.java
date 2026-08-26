package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlaceImportAnalysisWriter {

    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    public PlaceImportAnalysisWriter(ContentRepository contentRepository, ObjectMapper objectMapper) {
        this.contentRepository = contentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<List<ExtractedPlace>> loadCachedAnalysis(
            Long contentId, String contentHash, String analyzerModel, String promptVersion) {
        return contentRepository.findById(contentId)
                .filter(content -> contentHash.equals(content.getAnalysisContentHash()))
                .filter(content -> analyzerModel.equals(content.getAnalyzerModel()))
                .filter(content -> promptVersion.equals(content.getPromptVersion()))
                .filter(content -> content.getAnalyzedAt() != null)
                .flatMap(this::readCandidates);
    }

    @Transactional
    public String claimAnalysis(Long contentId, String contentHash, String analyzerModel,
                                String promptVersion, Instant now, Instant staleBefore) {
        String claimToken = UUID.randomUUID().toString();
        return contentRepository.claimAnalysis(contentId, contentHash, analyzerModel, promptVersion,
                claimToken, now, staleBefore) == 1 ? claimToken : null;
    }

    @Transactional
    public boolean saveAnalysis(Long contentId, String claimToken, String contentHash,
                                String analyzerModel, String promptVersion,
                                List<ExtractedPlace> candidates, Instant analyzedAt) {
        try {
            String candidatesJson = objectMapper.writeValueAsString(candidates);
            return contentRepository.completeAnalysis(contentId, claimToken, contentHash, analyzerModel,
                    promptVersion, candidatesJson, analyzedAt) == 1;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to cache place analysis", exception);
        }
    }

    @Transactional
    public void failAnalysis(Long contentId, String claimToken) {
        contentRepository.failAnalysis(contentId, claimToken);
    }

    private Optional<List<ExtractedPlace>> readCandidates(Content content) {
        try {
            ExtractedPlace[] candidates = objectMapper.readValue(
                    content.getExtractedCandidatesJson(), ExtractedPlace[].class);
            return Optional.of(List.of(candidates));
        } catch (Exception exception) {
            contentRepository.invalidateCachedAnalysis(content.getId());
            return Optional.empty();
        }
    }
}
