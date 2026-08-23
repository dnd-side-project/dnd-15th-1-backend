package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlace;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlaceContentsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentPlaceRepository contentPlaceRepository;

    @Test
    void returnsOnlyPublicContentsLinkedToPlace() throws Exception {
        Instant now = Instant.now();
        Member member = memberRepository.save(Member.create(now));
        IssuedTokens tokens = tokenService.issue(member);
        Place target = savePlace("대상 카페", now);
        Place other = savePlace("다른 카페", now);
        Content linkedPublic = saveContent("연결된 공개 게시물", true, now);
        Content linkedPending = saveContent("연결된 비공개 게시물", false, now);
        Content unlinkedPublic = saveContent("다른 장소 공개 게시물", true, now);
        contentPlaceRepository.save(ContentPlace.create(linkedPublic.getId(), target.getId(), now));
        contentPlaceRepository.save(ContentPlace.create(linkedPending.getId(), target.getId(), now));
        contentPlaceRepository.save(ContentPlace.create(unlinkedPublic.getId(), other.getId(), now));

        mockMvc.perform(get("/api/v1/places/{placeId}/contents", target.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents.length()").value(1))
                .andExpect(jsonPath("$.contents[0].contentId").value(linkedPublic.getId()))
                .andExpect(jsonPath("$.contents[0].title").value("연결된 공개 게시물"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsEmptyPageWhenPlaceHasNoPublicContents() throws Exception {
        Instant now = Instant.now();
        Member member = memberRepository.save(Member.create(now));
        IssuedTokens tokens = tokenService.issue(member);
        Place place = savePlace("게시물 없는 장소", now);

        mockMvc.perform(get("/api/v1/places/{placeId}/contents", place.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void returnsNotFoundWhenPlaceDoesNotExist() throws Exception {
        Instant now = Instant.now();
        Member member = memberRepository.save(Member.create(now));
        IssuedTokens tokens = tokenService.issue(member);

        mockMvc.perform(get("/api/v1/places/{placeId}/contents", 9_999_999L)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound());
    }

    private Place savePlace(String name, Instant now) {
        return placeRepository.save(Place.create(
                "kakao-" + UUID.randomUUID(),
                name,
                "서울특별시 성동구 성수동1가",
                "서울특별시 성동구 성수이로",
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000"),
                "음식점 > 카페",
                "CE7",
                null,
                now
        ));
    }

    private Content saveContent(String title, boolean publish, Instant now) {
        Content content = Content.create(
                "https://www.instagram.com/reel/" + UUID.randomUUID(),
                UUID.randomUUID().toString(),
                ContentSourceType.INSTAGRAM_REEL,
                title,
                "성수 카페",
                null,
                UUID.randomUUID().toString(),
                now
        );
        if (publish) {
            content.publish(now);
        }
        return contentRepository.save(content);
    }
}
