package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.security.config.OpsAccessProperties;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OperationsAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpsAccessProperties opsAccessProperties;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentImageRepository contentImageRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceImportRepository placeImportRepository;

    @Autowired
    private PlaceCandidateRepository placeCandidateRepository;

    @Autowired
    private SocialAccountService socialAccountService;

    @Test
    void rejectsOperationsApiWithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/contents/{contentId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsKakaoPlaceSearchWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/places/kakao-search")
                        .param("query", "도원반점"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void filtersPlacesByCategoryGroupAndThumbnailStatus() throws Exception {
        Place classified = createPlace();
        Place unclassified = placeRepository.save(Place.create(
                "ops-unclassified-" + UUID.randomUUID(),
                "운영자 미분류 장소",
                "서울특별시 강남구",
                "서울특별시 강남구 테헤란로",
                new BigDecimal("37.5046000"),
                new BigDecimal("127.0496000"),
                "음식점",
                null,
                null,
                Instant.now()
        ));

        String categoryResponse = mockMvc.perform(get("/api/v1/admin/places/search")
                        .param("categoryGroupCode", " ce7 ")
                        .with(operator()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(categoryResponse).contains("\"placeId\":" + classified.getId());
        assertThat(categoryResponse).doesNotContain("\"placeId\":" + unclassified.getId());

        String missingResponse = mockMvc.perform(get("/api/v1/admin/places/search")
                        .param("categoryGroupCode", "MISSING")
                        .param("thumbnailStatus", "MISSING")
                        .with(operator()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(missingResponse).contains("\"placeId\":" + unclassified.getId());
        assertThat(missingResponse).doesNotContain("\"placeId\":" + classified.getId());
    }

    @Test
    void returnsHistoricalMetricComparisonWhenPreviousPeriodHasNoActivity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/comparison")
                        .with(operator())
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriod").exists())
                .andExpect(jsonPath("$.previousPeriod").exists())
                .andExpect(jsonPath("$.metrics").isArray());
    }

    @Test
    void exposesSupportedKakaoCategoryGroupsToOperators() throws Exception {
        mockMvc.perform(get("/api/v1/admin/places/category-groups")
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("FD6"))
                .andExpect(jsonPath("$[0].name").value("음식점"))
                .andExpect(jsonPath("$[1].code").value("CE7"));
    }

    @Test
    void rejectsUnsupportedKakaoCategoryGroupWhenUpdatingPlace() throws Exception {
        Place place = createPlace();

        mockMvc.perform(patch("/api/v1/admin/places/{placeId}", place.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryGroupCode": "ZZ9",
                                  "expectedUpdatedAt": "%s"
                                }
                                """.formatted(place.getUpdatedAt())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void normalizesCategoryGroupAndRefreshesDulpickCategoryWhenUpdatingPlace() throws Exception {
        Place place = createPlace();

        mockMvc.perform(patch("/api/v1/admin/places/{placeId}", place.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryGroupCode": " ce7 ",
                                  "expectedUpdatedAt": "%s"
                                }
                                """.formatted(place.getUpdatedAt())))
                .andExpect(status().isOk());

        Place updated = placeRepository.findById(place.getId()).orElseThrow();
        assertThat(updated.getCategoryGroupCode()).isEqualTo("CE7");
        assertThat(updated.getStoredDulpickCategoryCode()).isEqualTo(DulpickPlaceCategory.CAFE);
    }

    @Test
    void rejectsStaleContentUpdateWithConflict() throws Exception {
        Content content = createContent();
        Instant expectedUpdatedAt = content.getUpdatedAt();

        mockMvc.perform(patch("/api/v1/admin/contents/{contentId}", content.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateContentJson(expectedUpdatedAt, "첫 번째 수정")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/contents/{contentId}", content.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateContentJson(expectedUpdatedAt, "오래된 수정")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADMIN_RESOURCE_MODIFIED"));
    }

    @Test
    void reordersContentImagesWithTheCurrentContentVersion() throws Exception {
        Content content = createContent();
        ContentImage first = ContentImage.create(
                content.getId(), "https://example.com/first.jpg", "first-hash", 0, content.getUpdatedAt()
        );
        ContentImage second = ContentImage.create(
                content.getId(), "https://example.com/second.jpg", "second-hash", 1, content.getUpdatedAt()
        );
        contentImageRepository.save(first);
        contentImageRepository.save(second);

        mockMvc.perform(patch("/api/v1/admin/contents/{contentId}/images/order", content.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageKeys": ["%s", "%s"],
                                  "expectedUpdatedAt": "%s"
                                }
                                """.formatted(second.getImageKey(), first.getImageKey(), content.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].imageKey").value(second.getImageKey()));
    }

    @Test
    void refusesToSetThumbnailWhenBackingFileIsMissing() throws Exception {
        Content content = createContent();
        ContentImage image = ContentImage.create(
                content.getId(), "https://example.com/missing.jpg", "missing-hash", 0, content.getUpdatedAt()
        );
        image.markStored(MediaType.IMAGE_JPEG.toString(), content.getUpdatedAt());
        contentImageRepository.save(image);

        mockMvc.perform(patch("/api/v1/admin/contents/{contentId}/images/{imageKey}/thumbnail",
                        content.getId(), image.getImageKey())
                        .with(operator())
                        .with(csrf())
                        .param("expectedUpdatedAt", content.getUpdatedAt().toString()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PUBLIC_CONTENT_IMAGE_UNAVAILABLE"));
    }

    @Test
    void manuallyLinksPlaceAndPublishesContent() throws Exception {
        Content content = createContent();
        Place place = createPlace();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "ops-import-" + UUID.randomUUID(),
                "ops-import@example.com",
                ProviderAuthorization.none()
        ).member();
        PlaceImport placeImport = PlaceImport.receive(
                member.getId(),
                content.getCanonicalUrl(),
                Sha256.hex(content.getCanonicalUrl()),
                ContentSourceType.INSTAGRAM_REEL,
                content.getCreatedAt()
        );
        placeImport.attachContent(content.getId());
        placeImport = placeImportRepository.save(placeImport);

        mockMvc.perform(post("/api/v1/admin/place-imports/{importId}/manual-place", placeImport.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placeId": %d,
                                  "publish": true,
                                  "expectedUpdatedAt": "%s"
                                }
                                """.formatted(place.getId(), placeImport.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLIC"))
                .andExpect(jsonPath("$.places[0].placeId").value(place.getId()));

        assertThat(placeImportRepository.findById(placeImport.getId()).orElseThrow()
                .getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void manuallyLinksPlaceWithoutPublishingAndCompletesImport() throws Exception {
        Content content = createContent();
        Place place = createPlace();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "ops-import-unpublished-" + UUID.randomUUID(),
                "ops-import-unpublished@example.com",
                ProviderAuthorization.none()
        ).member();
        PlaceImport placeImport = PlaceImport.receive(
                member.getId(),
                content.getCanonicalUrl(),
                Sha256.hex(content.getCanonicalUrl()),
                ContentSourceType.INSTAGRAM_REEL,
                content.getCreatedAt()
        );
        placeImport.attachContent(content.getId());
        placeImport = placeImportRepository.save(placeImport);

        mockMvc.perform(post("/api/v1/admin/place-imports/{importId}/manual-place", placeImport.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placeId": %d,
                                  "publish": false,
                                  "expectedUpdatedAt": "%s"
                                }
                                """.formatted(place.getId(), placeImport.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PENDING"));

        assertThat(placeImportRepository.findById(placeImport.getId()).orElseThrow()
                .getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void requiresEveryCandidateToBeReviewedBeforeFinalPublication() throws Exception {
        Content content = createContent();
        Place place = createPlace();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "ops-import-review-" + UUID.randomUUID(),
                "ops-import-review@example.com",
                ProviderAuthorization.none()
        ).member();
        PlaceImport placeImport = PlaceImport.receive(
                member.getId(),
                content.getCanonicalUrl(),
                Sha256.hex(content.getCanonicalUrl()),
                ContentSourceType.INSTAGRAM_REEL,
                content.getCreatedAt()
        );
        placeImport.attachContent(content.getId());
        placeImport = placeImportRepository.save(placeImport);
        PlaceCandidate candidate = placeCandidateRepository.save(PlaceCandidate.extracted(
                placeImport.getId(), "확인할 장소", "서울", null, "EXPLICIT_VENUE", Instant.now()
        ));

        mockMvc.perform(post("/api/v1/admin/place-imports/{importId}/manual-place", placeImport.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placeId": %d,
                                  "candidateId": %d,
                                  "publish": false,
                                  "expectedUpdatedAt": "%s"
                                }
                                """.formatted(place.getId(), candidate.getId(), placeImport.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PENDING"));

        PlaceImport linked = placeImportRepository.findById(placeImport.getId()).orElseThrow();
        mockMvc.perform(post("/api/v1/admin/place-imports/{importId}/complete", placeImport.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedUpdatedAt": "%s"}
                                """.formatted(linked.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLIC"));
    }

    @Test
    void canExcludeUnresolvedCandidateBeforeFinalPublication() throws Exception {
        Content content = createContent();
        Place place = createPlace();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "ops-import-reject-" + UUID.randomUUID(),
                "ops-import-reject@example.com",
                ProviderAuthorization.none()
        ).member();
        PlaceImport placeImport = PlaceImport.receive(
                member.getId(),
                content.getCanonicalUrl(),
                Sha256.hex(content.getCanonicalUrl()),
                ContentSourceType.INSTAGRAM_REEL,
                content.getCreatedAt()
        );
        placeImport.attachContent(content.getId());
        placeImport = placeImportRepository.save(placeImport);
        PlaceCandidate candidate = placeCandidateRepository.save(PlaceCandidate.extracted(
                placeImport.getId(), "없는 장소", "서울", null, "EXPLICIT_VENUE", Instant.now()
        ));

        mockMvc.perform(delete("/api/v1/admin/place-imports/{importId}/candidates/{candidateId}",
                        placeImport.getId(), candidate.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedUpdatedAt": "%s"}
                                """.formatted(placeImport.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].verificationStatus").value("REJECTED"));

        PlaceImport updated = placeImportRepository.findById(placeImport.getId()).orElseThrow();
        mockMvc.perform(post("/api/v1/admin/place-imports/{importId}/manual-place", placeImport.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placeId": %d,
                                  "publish": false,
                                  "expectedUpdatedAt": "%s"
                                }
                                """.formatted(place.getId(), updated.getUpdatedAt())))
                .andExpect(status().isOk());

        PlaceImport linked = placeImportRepository.findById(placeImport.getId()).orElseThrow();
        mockMvc.perform(post("/api/v1/admin/place-imports/{importId}/complete", placeImport.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedUpdatedAt": "%s"}
                                """.formatted(linked.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLIC"));
    }

    @Test
    void canLinkPlaceAndPublishPendingContentInOneOperation() throws Exception {
        Content content = createContent();
        Place place = createPlace();

        mockMvc.perform(patch("/api/v1/admin/contents/{contentId}/places", content.getId())
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placeIds": [%d],
                                  "expectedUpdatedAt": "%s",
                                  "publish": true
                                }
                                """.formatted(place.getId(), content.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLIC"))
                .andExpect(jsonPath("$.places[0].placeId").value(place.getId()));
    }

    private Content createContent() {
        Instant now = Instant.now();
        Content content = Content.create(
                "https://www.instagram.com/reel/" + UUID.randomUUID(),
                UUID.randomUUID().toString(),
                ContentSourceType.INSTAGRAM_REEL,
                "운영자 테스트 콘텐츠",
                "테스트 본문",
                null,
                UUID.randomUUID().toString(),
                now
        );
        return contentRepository.save(content);
    }

    private Place createPlace() {
        Instant now = Instant.now();
        return placeRepository.save(Place.create(
                "ops-place-" + UUID.randomUUID(),
                "운영자 테스트 장소",
                "서울특별시 성동구 성수동",
                "서울특별시 성동구 성수이로",
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000"),
                "음식점 > 카페",
                "CE7",
                null,
                now
        ));
    }

    @Test
    void returnsDailyStatsWithinConfiguredWindow() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/daily")
                        .param("days", "14")
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats").isArray());
    }

    @Test
    void exposesAnalyticsMetricsToAuthenticatedOperator() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/overview")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-03")
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").exists());

        mockMvc.perform(get("/api/v1/admin/metrics/trends")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-03")
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").isArray());

        mockMvc.perform(get("/api/v1/admin/metrics/funnel")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-03")
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps").isArray());

        mockMvc.perform(get("/api/v1/admin/metrics/retention")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-03")
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods").isArray());
    }

    @Test
    void rejectsAnalyticsMetricsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAnalyticsMetricsForAnInvalidOrExcessivelyLargePeriod() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/overview")
                        .param("from", "2026-09-03")
                        .param("to", "2026-09-01")
                        .with(operator()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        mockMvc.perform(get("/api/v1/admin/metrics/overview")
                        .param("from", "2025-01-01")
                        .param("to", "2027-01-01")
                        .with(operator()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void createsAdminPlaceIdempotently() throws Exception {
        String kakaoPlaceId = "ops-create-" + UUID.randomUUID();
        String body = """
                {
                  "kakaoPlaceId": "%s",
                  "name": "운영자 신규 장소",
                  "address": "서울특별시 강남구"
                }
                """.formatted(kakaoPlaceId);

        mockMvc.perform(post("/api/v1/admin/places")
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kakaoPlaceId").value(kakaoPlaceId))
                .andExpect(jsonPath("$.placeId").isNumber());

        mockMvc.perform(post("/api/v1/admin/places")
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertThat(placeRepository.findByKakaoPlaceId(kakaoPlaceId)).isPresent();
    }

    @Test
    void filtersImportsWithUnverifiedCandidates() throws Exception {
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "ops-unverified-" + UUID.randomUUID(),
                "ops-unverified@example.com",
                ProviderAuthorization.none()
        ).member();

        Content partialContent = createContent();
        PlaceImport partialImport = PlaceImport.receive(
                member.getId(),
                partialContent.getCanonicalUrl(),
                Sha256.hex(partialContent.getCanonicalUrl()),
                ContentSourceType.INSTAGRAM_REEL,
                Instant.now()
        );
        partialImport.attachContent(partialContent.getId());
        partialImport = placeImportRepository.save(partialImport);
        placeCandidateRepository.save(PlaceCandidate.extracted(
                partialImport.getId(), "미검증 후보", null, null, "EXPLICIT_VENUE", Instant.now()
        ));

        Content cleanContent = createContent();
        PlaceImport cleanImport = PlaceImport.receive(
                member.getId(),
                cleanContent.getCanonicalUrl(),
                Sha256.hex(cleanContent.getCanonicalUrl()),
                ContentSourceType.INSTAGRAM_REEL,
                Instant.now()
        );
        cleanImport.attachContent(cleanContent.getId());
        placeImportRepository.save(cleanImport);

        String response = mockMvc.perform(get("/api/v1/admin/place-imports")
                        .param("hasUnverified", "true")
                        .with(operator()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("\"importId\":" + partialImport.getId());
        assertThat(response).doesNotContain("\"importId\":" + cleanImport.getId());

        mockMvc.perform(get("/api/v1/admin/place-imports/{importId}", partialImport.getId())
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.canonicalUrl").value(partialContent.getCanonicalUrl()))
                .andExpect(jsonPath("$.summary.failedPlaceNames").value("미검증 후보"));
    }

    @Test
    void requeuesReviewRequiredImportForOperatorRetry() throws Exception {
        Content content = createContent();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "ops-import-retry-review-" + UUID.randomUUID(),
                "ops-import-retry-review@example.com",
                ProviderAuthorization.none()
        ).member();
        PlaceImport placeImport = PlaceImport.receive(
                member.getId(),
                content.getCanonicalUrl(),
                Sha256.hex(content.getCanonicalUrl()),
                ContentSourceType.INSTAGRAM_REEL,
                content.getCreatedAt()
        );
        placeImport.attachContent(content.getId());
        placeImport.complete("제목", "본문", null, "hash", Instant.now(), Instant.now(), true);
        placeImport = placeImportRepository.saveAndFlush(placeImport);

        mockMvc.perform(post("/api/v1/admin/place-imports/{importId}/retry", placeImport.getId())
                        .with(operator())
                        .with(csrf()))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/admin/place-imports/{importId}", placeImport.getId())
                        .with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("RECEIVED"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor operator() {
        return httpBasic(opsAccessProperties.username(), opsAccessProperties.password());
    }

    private String updateContentJson(Instant expectedUpdatedAt, String title) {
        return """
                {
                  "title": "%s",
                  "content": "운영자 수정 본문",
                  "expectedUpdatedAt": "%s"
                }
                """.formatted(title, expectedUpdatedAt);
    }
}
