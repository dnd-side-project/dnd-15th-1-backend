package kr.omong.dulpick.domain.search;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.search.application.RecentSearchCommandService;
import kr.omong.dulpick.domain.search.application.RecentSearchQueryService;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecentSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RecentSearchCommandService commandService;

    @Autowired
    private RecentSearchQueryService queryService;

    @Test
    void upsertsSameKeywordAndKeepsSearchTypesSeparateWithoutRetentionLimit() {
        Member member = memberRepository.save(Member.create(Instant.now()));

        commandService.record(member.getId(), RecentSearchType.PLACE, "Seoul");
        commandService.record(member.getId(), RecentSearchType.PLACE, "  seoul  ");
        commandService.record(member.getId(), RecentSearchType.CONTENT, "Seoul");

        var placeHistory = queryService.getRecentSearches(
                member.getId(),
                RecentSearchType.PLACE,
                PageRequest.of(0, 20)
        );
        var contentHistory = queryService.getRecentSearches(
                member.getId(),
                RecentSearchType.CONTENT,
                PageRequest.of(0, 20)
        );

        assertThat(placeHistory.getTotalElements()).isEqualTo(1);
        assertThat(placeHistory.getContent()).singleElement().satisfies(history ->
                assertThat(history.keyword()).isEqualTo("seoul")
        );
        assertThat(contentHistory.getTotalElements()).isEqualTo(1);
        assertThat(contentHistory.getContent()).singleElement().satisfies(history ->
                assertThat(history.keyword()).isEqualTo("Seoul")
        );
    }

    @Test
    void exposesAuthenticatedRecentSearchCrudApi() throws Exception {
        Member member = memberRepository.save(Member.create(Instant.now()));
        IssuedTokens tokens = tokenService.issue(member);
        String authorization = "Bearer " + tokens.accessToken();

        mockMvc.perform(post("/api/v1/recent-searches")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PLACE",
                                  "keyword": "성수 카페"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PLACE"))
                .andExpect(jsonPath("$.keyword").value("성수 카페"));

        mockMvc.perform(get("/api/v1/recent-searches")
                        .header("Authorization", authorization)
                        .param("type", "PLACE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentSearches.length()").value(1))
                .andExpect(jsonPath("$.recentSearches[0].keyword").value("성수 카페"));

        mockMvc.perform(delete("/api/v1/recent-searches")
                        .header("Authorization", authorization)
                        .param("type", "PLACE"))
                .andExpect(status().isNoContent());
    }
}
