package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.application.PlaceKeywordSearch;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.infrastructure.KakaoPlaceSearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlaceMapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private MemberPlaceRepository memberPlaceRepository;

    @MockitoBean
    private KakaoPlaceSearchClient placeSearcher;

    @Test
    void filtersSavedMapPlacesAndReturnsIntegratedDetail() throws Exception {
        Instant now = Instant.now();
        Member member = memberRepository.save(Member.create(now));
        IssuedTokens tokens = tokenService.issue(member);
        String kakaoPlaceId = "map-" + UUID.randomUUID();
        String searchKeyword = "mapsearch" + UUID.randomUUID().toString().replace("-", "");
        String placeName = searchKeyword + " 성수 지도 카페";
        Place place = placeRepository.save(Place.create(
                kakaoPlaceId,
                placeName,
                "서울특별시 성동구 성수동1가",
                "서울특별시 성동구 성수이로",
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000"),
                "음식점 > 카페",
                "CE7",
                "02-1234-5678",
                "https://place.map.kakao.com/" + kakaoPlaceId,
                null,
                now
        ));
        memberPlaceRepository.save(MemberPlace.save(
                member.getId(),
                place,
                null,
                null,
                now
        ));
        String authorization = "Bearer " + tokens.accessToken();

        mockMvc.perform(get("/api/v1/places")
                        .header("Authorization", authorization)
                        .param("category", "CAFE")
                        .param("ownershipStatus", "MINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value(place.getId()))
                .andExpect(jsonPath("$[0].kakaoPlaceId").value(kakaoPlaceId))
                .andExpect(jsonPath("$[0].ownershipStatus").value("MINE"));

        mockMvc.perform(get("/api/v1/places/{placeId}", place.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.savedByMe").value(true))
                .andExpect(jsonPath("$.ownershipStatus").value("MINE"));

        when(placeSearcher.search(searchKeyword, 1)).thenReturn(new PlaceKeywordSearch(
                java.util.List.of(
                new PlaceSearchResult(
                        kakaoPlaceId,
                        "Kakao의 다른 장소명",
                        place.getAddress(),
                        place.getRoadAddress(),
                        place.getLatitude(),
                        place.getLongitude(),
                        "CE7",
                        "음식점 > 카페",
                        "02-0000-0000",
                        "https://place.map.kakao.com/" + kakaoPlaceId,
                        null
                ),
                new PlaceSearchResult(
                        "external-" + UUID.randomUUID(),
                        searchKeyword + " 외부 카페",
                        "서울특별시 성동구 성수동2가",
                        "서울특별시 성동구 성수길",
                        place.getLatitude(),
                        place.getLongitude(),
                        "CE7",
                        "음식점 > 카페",
                        null,
                        null,
                        null
                )
        ), true));
        mockMvc.perform(get("/api/v1/places/search")
                        .header("Authorization", authorization)
                        .param("query", searchKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.places[0].placeId").value(place.getId()))
                .andExpect(jsonPath("$.places[0].name").value(placeName))
                .andExpect(jsonPath("$.places[0].ownershipStatus").value("MINE"))
                .andExpect(jsonPath("$.places[1].placeId").doesNotExist());
    }
}
