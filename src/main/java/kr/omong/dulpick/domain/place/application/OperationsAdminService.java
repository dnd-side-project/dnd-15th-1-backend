package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentPublicationStatusRequest;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationsAdminService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Duration STALE_IMPORT_TIMEOUT = Duration.ofMinutes(10);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final PlaceImportRepository placeImportRepository;
    private final PlaceImportDispatcher placeImportDispatcher;
    private final ContentRepository contentRepository;
    private final ContentImageRepository contentImageRepository;
    private final ContentImageEnrichmentService contentImageEnrichmentService;
    private final PlaceRepository placeRepository;
    private final PlaceImageEnrichmentDispatcher placeImageEnrichmentDispatcher;

    public OperationsAdminService(
            JdbcTemplate jdbcTemplate,
            Clock clock,
            PlaceImportRepository placeImportRepository,
            PlaceImportDispatcher placeImportDispatcher,
            ContentRepository contentRepository,
            ContentImageRepository contentImageRepository,
            ContentImageEnrichmentService contentImageEnrichmentService,
            PlaceRepository placeRepository,
            PlaceImageEnrichmentDispatcher placeImageEnrichmentDispatcher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.placeImportRepository = placeImportRepository;
        this.placeImportDispatcher = placeImportDispatcher;
        this.contentRepository = contentRepository;
        this.contentImageRepository = contentImageRepository;
        this.contentImageEnrichmentService = contentImageEnrichmentService;
        this.placeRepository = placeRepository;
        this.placeImageEnrichmentDispatcher = placeImageEnrichmentDispatcher;
    }

    @Transactional(readOnly = true)
    public OperationsAdminView.Dashboard dashboard() {
        Instant since = clock.instant().minus(Duration.ofHours(24));
        long imports = count("SELECT COUNT(*) FROM place_imports WHERE created_at >= ?", since);
        long completed = count("SELECT COUNT(*) FROM place_imports WHERE created_at >= ? AND status = 'COMPLETED'", since);
        long reviewRequired = count("SELECT COUNT(*) FROM place_imports WHERE created_at >= ? AND status = 'REVIEW_REQUIRED'", since);
        long failed = count("SELECT COUNT(*) FROM place_imports WHERE created_at >= ? AND status = 'FAILED'", since);
        long stale = count(
                "SELECT COUNT(*) FROM place_imports WHERE status = 'PROCESSING' AND updated_at < ?",
                clock.instant().minus(STALE_IMPORT_TIMEOUT)
        );
        long pendingContents = count("SELECT COUNT(*) FROM contents WHERE publication_status = 'PENDING'");
        long contentBacklogs = count("SELECT COUNT(*) FROM content_image_enrichment_backlogs WHERE status IN ('PENDING', 'PROCESSING')");
        long placeBacklogs = count("SELECT COUNT(*) FROM place_image_enrichment_backlogs WHERE status = 'PENDING'");
        long[] duration = duration(since);
        return new OperationsAdminView.Dashboard(
                imports,
                completed,
                reviewRequired,
                failed,
                stale,
                pendingContents,
                contentBacklogs,
                placeBacklogs,
                duration[0],
                duration[1],
                grouped("SELECT failure_code AS item, COUNT(*) AS total FROM place_imports "
                        + "WHERE created_at >= ? AND failure_code IS NOT NULL GROUP BY failure_code ORDER BY total DESC", since),
                grouped("SELECT source_type AS item, COUNT(*) AS total FROM place_imports "
                        + "WHERE created_at >= ? GROUP BY source_type ORDER BY total DESC", since)
        );
    }

    @Transactional(readOnly = true)
    public OperationsAdminView.ImportPage imports(
            PlaceImportStatus status,
            String failureCode,
            String query,
            int page,
            int size
    ) {
        PageBounds bounds = bounds(page, size);
        QueryParts parts = importQuery(status, failureCode, query);
        long total = count(parts.countSql(), parts.parameters().toArray());
        List<OperationsAdminView.ImportSummary> imports = jdbcTemplate.query(
                parts.sql() + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                ps -> setParameters(ps, parts.parameters(), bounds.size(), bounds.offset()),
                (rs, rowNum) -> importSummary(rs)
        );
        return new OperationsAdminView.ImportPage(
                imports,
                bounds.page(),
                bounds.size(),
                total,
                totalPages(total, bounds.size()),
                bounds.offset() + imports.size() < total
        );
    }

    @Transactional(readOnly = true)
    public OperationsAdminView.ImportDetail importDetail(Long importId) {
        OperationsAdminView.ImportSummary summary = jdbcTemplate.query(
                "SELECT id, content_id, source_type, canonical_url, status, failure_code, retry_count, "
                        + "created_at, updated_at, completed_at FROM place_imports WHERE id = ?",
                (rs, rowNum) -> importSummary(rs),
                importId
        ).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.PLACE_IMPORT_NOT_FOUND));
        ContentData content = summary.contentId() == null
                ? new ContentData(null, null, null)
                : jdbcTemplate.query(
                        "SELECT title, content, thumbnail_url FROM contents WHERE id = ?",
                        (rs, rowNum) -> new ContentData(
                                rs.getString("title"),
                                rs.getString("content"),
                                rs.getString("thumbnail_url")
                        ),
                        summary.contentId()
                ).stream().findFirst().orElse(new ContentData(null, null, null));
        List<OperationsAdminView.Candidate> candidates = jdbcTemplate.query(
                "SELECT candidate.id, candidate.extracted_name, candidate.extracted_address_hint, "
                        + "candidate.verification_status, candidate.place_id, place.name, place.address "
                        + "FROM place_candidates candidate LEFT JOIN places place ON place.id = candidate.place_id "
                        + "WHERE candidate.import_id = ? ORDER BY candidate.id",
                (rs, rowNum) -> new OperationsAdminView.Candidate(
                        rs.getLong("id"),
                        rs.getString("extracted_name"),
                        rs.getString("extracted_address_hint"),
                        rs.getString("verification_status"),
                        nullableLong(rs, "place_id"),
                        rs.getString("name"),
                        rs.getString("address")
                ),
                importId
        );
        return new OperationsAdminView.ImportDetail(
                summary,
                content.title(),
                content.caption(),
                content.thumbnailUrl(),
                candidates
        );
    }

    @Transactional
    public void retryImport(Long importId) {
        Instant now = clock.instant();
        int updated = placeImportRepository.adminRequeue(
                importId,
                now,
                now.minus(STALE_IMPORT_TIMEOUT)
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        dispatchAfterCommit(() -> placeImportDispatcher.dispatch(importId));
    }

    @Transactional(readOnly = true)
    public OperationsAdminView.ContentPage contents(
            ContentPublicationStatus status,
            String query,
            int page,
            int size
    ) {
        PageBounds bounds = bounds(page, size);
        List<Object> parameters = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (status != null) {
            where.append(" AND publication_status = ?");
            parameters.add(status.name());
        }
        if (query != null && !query.isBlank()) {
            where.append(" AND (title LIKE ? OR canonical_url LIKE ?)");
            parameters.add("%" + query.strip() + "%");
            parameters.add("%" + query.strip() + "%");
        }
        long total = count("SELECT COUNT(*) FROM contents" + where, parameters.toArray());
        parameters.add(bounds.size());
        parameters.add(bounds.offset());
        List<OperationsAdminView.ContentSummary> contents = jdbcTemplate.query(
                "SELECT id, source_type, canonical_url, title, publication_status, place_count, thumbnail_url, "
                        + "created_at, updated_at FROM contents" + where
                        + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                ps -> setParameters(ps, parameters),
                (rs, rowNum) -> new OperationsAdminView.ContentSummary(
                        rs.getLong("id"),
                        ContentSourceType.valueOf(rs.getString("source_type")),
                        rs.getString("canonical_url"),
                        rs.getString("title"),
                        ContentPublicationStatus.valueOf(rs.getString("publication_status")),
                        rs.getInt("place_count"),
                        rs.getString("thumbnail_url"),
                        instant(rs, "created_at"),
                        instant(rs, "updated_at")
                )
        );
        return new OperationsAdminView.ContentPage(
                contents,
                bounds.page(),
                bounds.size(),
                total,
                totalPages(total, bounds.size()),
                bounds.offset() + contents.size() < total
        );
    }

    @Transactional
    public OperationsAdminView.ContentSummary updatePublicationStatus(
            Long contentId,
            UpdateContentPublicationStatusRequest request
    ) {
        var content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PUBLIC_CONTENT_NOT_FOUND));
        content.updatePublicationStatus(request.publicationStatus(), clock.instant());
        contentRepository.save(content);
        return new OperationsAdminView.ContentSummary(
                content.getId(),
                content.getSourceType(),
                content.getCanonicalUrl(),
                content.getTitle(),
                content.getPublicationStatus(),
                content.getPlaceCount(),
                content.getThumbnailUrl(),
                content.getCreatedAt(),
                content.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public OperationsAdminView.ImageBacklogPage imageBacklogs(String kind, int page, int size) {
        PageBounds bounds = bounds(page, size);
        String normalizedKind = kind == null ? "ALL" : kind.strip().toUpperCase();
        if (!List.of("ALL", "CONTENT", "PLACE").contains(normalizedKind)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String union = "SELECT * FROM ("
                + "SELECT 'CONTENT' AS kind, id AS resource_id, content_id, NULL AS place_id, status, "
                + "'CONTENT_IMAGE' AS reason, attempt_count, updated_at AS last_attempt_at "
                + "FROM content_image_enrichment_backlogs "
                + "UNION ALL "
                + "SELECT 'PLACE' AS kind, id AS resource_id, NULL AS content_id, place_id, status, reason, "
                + "attempt_count, last_failed_at AS last_attempt_at FROM place_image_enrichment_backlogs"
                + ") backlog WHERE status IN ('PENDING', 'PROCESSING')";
        List<Object> parameters = new ArrayList<>();
        if (!"ALL".equals(normalizedKind)) {
            union += " AND kind = ?";
            parameters.add(normalizedKind);
        }
        long total = count("SELECT COUNT(*) FROM (" + union + ") count_query", parameters.toArray());
        parameters.add(bounds.size());
        parameters.add(bounds.offset());
        List<OperationsAdminView.ImageBacklog> backlogs = jdbcTemplate.query(
                union + " ORDER BY last_attempt_at ASC, resource_id ASC LIMIT ? OFFSET ?",
                ps -> setParameters(ps, parameters),
                (rs, rowNum) -> new OperationsAdminView.ImageBacklog(
                        rs.getString("kind"),
                        rs.getLong("resource_id"),
                        nullableLong(rs, "content_id"),
                        nullableLong(rs, "place_id"),
                        rs.getString("status"),
                        rs.getString("reason"),
                        rs.getInt("attempt_count"),
                        instant(rs, "last_attempt_at")
                )
        );
        return new OperationsAdminView.ImageBacklogPage(
                backlogs,
                bounds.page(),
                bounds.size(),
                total,
                totalPages(total, bounds.size()),
                bounds.offset() + backlogs.size() < total
        );
    }

    @Transactional(readOnly = true)
    public void retryContentImages(Long contentId) {
        if (!contentRepository.existsById(contentId)) {
            throw new BusinessException(ErrorCode.PUBLIC_CONTENT_NOT_FOUND);
        }
        List<String> sourceUrls = contentImageRepository.findAllByContentIdOrderByDisplayOrderAsc(contentId)
                .stream()
                .map(ContentImage::getSourceUrl)
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();
        if (sourceUrls.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        contentImageEnrichmentService.dispatch(contentId, sourceUrls);
    }

    @Transactional(readOnly = true)
    public void retryPlaceImages(Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
        placeImageEnrichmentDispatcher.dispatchPlace(placeId);
    }

    private QueryParts importQuery(PlaceImportStatus status, String failureCode, String query) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (status != null) {
            where.append(" AND status = ?");
            parameters.add(status.name());
        }
        if (failureCode != null && !failureCode.isBlank()) {
            where.append(" AND failure_code = ?");
            parameters.add(failureCode.strip());
        }
        if (query != null && !query.isBlank()) {
            where.append(" AND (canonical_url LIKE ? OR title LIKE ?)");
            parameters.add("%" + query.strip() + "%");
            parameters.add("%" + query.strip() + "%");
        }
        String columns = " FROM place_imports" + where;
        return new QueryParts(
                "SELECT id, content_id, source_type, canonical_url, status, failure_code, retry_count, "
                        + "created_at, updated_at, completed_at" + columns,
                "SELECT COUNT(*)" + columns,
                parameters
        );
    }

    private OperationsAdminView.ImportSummary importSummary(ResultSet rs) throws SQLException {
        return new OperationsAdminView.ImportSummary(
                rs.getLong("id"),
                nullableLong(rs, "content_id"),
                ContentSourceType.valueOf(rs.getString("source_type")),
                rs.getString("canonical_url"),
                PlaceImportStatus.valueOf(rs.getString("status")),
                rs.getString("failure_code"),
                rs.getInt("retry_count"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                instant(rs, "completed_at")
        );
    }

    private long count(String sql, Object... parameters) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, parameters);
        return value == null ? 0L : value;
    }

    private Map<String, Long> grouped(String sql, Object... parameters) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            result.put(rs.getString("item"), rs.getLong("total"));
            return null;
        }, parameters);
        return result;
    }

    private long[] duration(Instant since) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(TIMESTAMPDIFF(MICROSECOND, created_at, completed_at) / 1000), 0), "
                        + "COALESCE(MAX(TIMESTAMPDIFF(MICROSECOND, created_at, completed_at) / 1000), 0) "
                        + "FROM place_imports WHERE created_at >= ? AND completed_at IS NOT NULL",
                (rs, rowNum) -> new long[]{rs.getLong(1), rs.getLong(2)},
                since
        );
    }

    private void dispatchAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private PageBounds bounds(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return new PageBounds(Math.max(page, 0), boundedSize, Math.max(page, 0) * boundedSize);
    }

    private int totalPages(long total, int size) {
        return (int) ((total + size - 1) / size);
    }

    private void setParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private void setParameters(
            PreparedStatement statement,
            List<Object> parameters,
            int size,
            int offset
    ) throws SQLException {
        setParameters(statement, parameters);
        statement.setInt(parameters.size() + 1, size);
        statement.setInt(parameters.size() + 2, offset);
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private record PageBounds(int page, int size, int offset) {
    }

    private record QueryParts(String sql, String countSql, List<Object> parameters) {
    }

    private record ContentData(String title, String caption, String thumbnailUrl) {
    }
}
