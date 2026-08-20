package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContentSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ContentRepository contentRepository;

    @Test
    void searchesOnlyPublishedContentByTitleOrBody() throws Exception {
        Instant now = Instant.now();
        String uniqueKeyword = "검색" + UUID.randomUUID().toString().replace("-", "");
        String title = uniqueKeyword + " 서울 데이트 추천";
        Member member = memberRepository.save(Member.create(now));
        IssuedTokens tokens = tokenService.issue(member);
        Content content = Content.create(
                "https://www.instagram.com/reel/" + UUID.randomUUID(),
                UUID.randomUUID().toString(),
                ContentSourceType.INSTAGRAM_REEL,
                title,
                "성수에서 즐기는 카페 데이트",
                null,
                UUID.randomUUID().toString(),
                now
        );
        content.publish(now);
        contentRepository.save(content);

        mockMvc.perform(get("/api/v1/contents/search")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .param("query", uniqueKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents.length()").value(1))
                .andExpect(jsonPath("$.contents[0].title").value(title))
                .andExpect(jsonPath("$.popularTags[0]").value("성수"))
                .andExpect(jsonPath("$.popularTags[1]").value("강남"))
                .andExpect(jsonPath("$.popularTags[2]").value("을지로"));

        mockMvc.perform(get("/api/v1/contents/search")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .param("query", "없음" + UUID.randomUUID().toString().replace("-", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents.length()").value(0));
    }
}
