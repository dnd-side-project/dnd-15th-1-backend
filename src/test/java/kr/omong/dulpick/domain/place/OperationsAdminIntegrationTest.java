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
