package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlace;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.security.config.OpsAccessProperties;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlaceClassificationAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpsAccessProperties opsAccessProperties;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentPlaceRepository contentPlaceRepository;

    @Test
    void listsOnlyPlacesLinkedToSavedContentAndClassifiesWithBasicAuth() throws Exception {
        Instant now = Instant.now();
        String uniqueName = "ops-class-" + UUID.randomUUID();
        Place linked = placeRepository.save(Place.create(
                "ops-linked-" + UUID.randomUUID(),
                uniqueName,
                "서울특별시 성동구 성수동1가",
                "서울특별시 성동구 성수이로",
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000"),
                "음식점 > 카페",
                "CE7",
                null,
                now
        ));
        Place unlinked = placeRepository.save(Place.create(
                "ops-unlinked-" + UUID.randomUUID(),
                uniqueName + "-단독",
                "서울특별시 성동구 성수동1가",
                "서울특별시 성동구 성수이로",
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000"),
                "음식점 > 카페",
                "CE7",
                null,
                now
        ));
        Content content = Content.create(
                "https://www.instagram.com/reel/" + UUID.randomUUID(),
                UUID.randomUUID().toString(),
                ContentSourceType.INSTAGRAM_REEL,
                uniqueName + " 콘텐츠",
                "성수 카페",
                null,
                UUID.randomUUID().toString(),
                now
        );
        content.publish(now);
        content = contentRepository.save(content);
        contentPlaceRepository.save(ContentPlace.create(content.getId(), linked.getId(), now));

        mockMvc.perform(get("/api/v1/admin/place-classifications")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .param("status", "UNCLASSIFIED")
                        .param("query", uniqueName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].placeId").value(linked.getId()))
                .andExpect(jsonPath("$.places[0].status").value("UNCLASSIFIED"))
                .andExpect(jsonPath("$.places[0].environment.value").value(nullValue()));

        mockMvc.perform(get("/api/v1/admin/place-classifications")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .param("query", uniqueName + "-단독"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0));

        mockMvc.perform(patch("/api/v1/admin/place-classifications/{placeId}", linked.getId())
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "INDOOR",
                                  "activity": "STATIC",
                                  "time": "DAY",
                                  "focus": "FOOD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLASSIFIED"))
                .andExpect(jsonPath("$.environment.value").value("INDOOR"))
                .andExpect(jsonPath("$.environment.source").value("MANUAL"));

        mockMvc.perform(get("/api/v1/admin/place-classifications")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .param("status", "UNCLASSIFIED")
                        .param("query", uniqueName)
                        .param("page", "0")
                        .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.counts.classified").value(1));

        mockMvc.perform(patch("/api/v1/admin/place-classifications/{placeId}", linked.getId())
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "time": null }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_CLASSIFIED"))
                .andExpect(jsonPath("$.time.value").value(nullValue()))
                .andExpect(jsonPath("$.environment.value").value("INDOOR"));

        mockMvc.perform(get("/api/v1/admin/place-classifications/{placeId}", unlinked.getId())
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value(unlinked.getId()));
    }

    @Test
    void rejectsOperatorApiWithoutAuthAndRedirectsHtmlToLogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/place-classifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ops/places").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/ops/login"));
    }

    @Test
    void rejectsAuthenticatedOperatorMutationWithoutCsrfToken() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/place-classifications/{placeId}", 1L)
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void servesOperatorLoginPageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/ops/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(content().string(containsString("운영자 계정으로 로그인")));
    }

    @Test
    void servesOperatorPageWithBasicAuth() throws Exception {
        mockMvc.perform(get("/ops/places")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("콘텐츠 장소 분류")))
                .andExpect(content().string(containsString("분류 완료")))
                .andExpect(content().string(containsString("textContent")));
    }

    @Test
    void servesOperationsDashboardWithContentAndImageManagement() throws Exception {
        mockMvc.perform(get("/ops")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("게시글 관리")))
                .andExpect(content().string(containsString("이미지 백로그")))
                .andExpect(content().string(containsString("/api/v1/admin/contents/")));
    }
}
