package kr.omong.dulpick.domain.date;

import com.jayway.jsonpath.JsonPath;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.couple.application.command.ConnectCoupleCommand;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.time.ServiceTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DateCourseIntegrationTest {

    private static final DatePreferences PREFERENCES = new DatePreferences(
            DatePreferenceOption.INDOOR,
            DatePreferenceOption.ACTIVE,
            DatePreferenceOption.NIGHT,
            DatePreferenceOption.FOOD
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private CoupleCommandService coupleCommandService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private MemberPlaceRepository memberPlaceRepository;

    @Test
    void createsAndConfirmsDateCourseAndExposesHomeSummary() throws Exception {
        CoupleFixture fixture = createConnectedCouple();
        Place first = savePlaceForMember(fixture.first().member().getId(), "first", Instant.now());
        Place second = savePlaceForMember(fixture.second().member().getId(), "second", Instant.now());

        mockMvc.perform(get("/api/v1/date-courses/places")
                        .header("Authorization", bearer(fixture.first())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.availableCategories.length()").isNotEmpty());

        mockMvc.perform(get("/api/v1/date-courses/places")
                        .header("Authorization", bearer(fixture.first()))
                        .param("region", "성동구"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(2));

        LocalDate futureDate = LocalDate.now(ServiceTime.ZONE_ID).plusDays(3);
        MvcResult createResult = mockMvc.perform(post("/api/v1/date-courses")
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"성수동 데이트",
                                  "date":"%s",
                                  "time":"19:30:00"
                                }
                                """.formatted(futureDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        Long dateCourseId = readLongFromJson(createResult, "$.dateCourseId");

        mockMvc.perform(put("/api/v1/date-courses/{dateCourseId}", dateCourseId)
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":0,
                                  "title":"성수동 데이트",
                                  "date":"%s",
                                  "time":"19:30:00",
                                  "placeIds":[%d, %d]
                                }
                                """.formatted(futureDate, first.getId(), second.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPlaceCount").value(2))
                .andExpect(jsonPath("$.places[0].order").value(1))
                .andExpect(jsonPath("$.places[1].order").value(2))
                .andExpect(jsonPath("$.places[0].walkToNext.distanceMeters").value(0))
                .andExpect(jsonPath("$.places[0].walkToNext.durationSeconds").value(0))
                .andExpect(jsonPath("$.places[1].walkToNext").doesNotExist());

        mockMvc.perform(get("/api/v1/date-courses/{dateCourseId}", dateCourseId)
                        .header("Authorization", bearer(fixture.first())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].walkToNext.distanceMeters").value(0))
                .andExpect(jsonPath("$.places[1].walkToNext").doesNotExist());

        mockMvc.perform(get("/api/v1/date-courses/current")
                        .header("Authorization", bearer(fixture.first())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDateCourse.dateCourseId").value(dateCourseId))
                .andExpect(jsonPath("$.currentDateCourse.totalPlaceCount").value(2));

        mockMvc.perform(get("/api/v1/home")
                        .header("Authorization", bearer(fixture.first())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.myNickname").value("첫번째"))
                .andExpect(jsonPath("$.partnerNickname").value("두번째"))
                .andExpect(jsonPath("$.currentDateCourse.dateCourseId").value(dateCourseId));

        mockMvc.perform(get("/api/v1/home/recent-saved-places")
                        .header("Authorization", bearer(fixture.first()))
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").isNotEmpty());
    }

    @Test
    void createsDateCourseWithoutTime() throws Exception {
        CoupleFixture fixture = createConnectedCouple();
        LocalDate futureDate = LocalDate.now(ServiceTime.ZONE_ID).plusDays(5);

        mockMvc.perform(post("/api/v1/date-courses")
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"날짜만 데이트",
                                  "date":"%s"
                                }
                                """.formatted(futureDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.date").value(futureDate.toString()))
                .andExpect(jsonPath("$.time").doesNotExist());
    }

    @Test
    void savesDateCourseWithoutTime() throws Exception {
        CoupleFixture fixture = createConnectedCouple();
        Place first = savePlaceForMember(fixture.first().member().getId(), "first", Instant.now());
        LocalDate futureDate = LocalDate.now(ServiceTime.ZONE_ID).plusDays(6);
        MvcResult createResult = mockMvc.perform(post("/api/v1/date-courses")
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"날짜만 확정 데이트",
                                  "date":"%s"
                                }
                                """.formatted(futureDate)))
                .andExpect(status().isCreated())
                .andReturn();
        Long dateCourseId = readLongFromJson(createResult, "$.dateCourseId");

        mockMvc.perform(put("/api/v1/date-courses/{dateCourseId}", dateCourseId)
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":0,
                                  "title":"날짜만 확정 데이트",
                                  "date":"%s",
                                  "placeIds":[%d]
                                }
                                """.formatted(futureDate, first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.date").value(futureDate.toString()))
                .andExpect(jsonPath("$.time").doesNotExist());
    }

    @Test
    void rejectsPlaceOutsideCoupleSavedPool() throws Exception {
        CoupleFixture fixture = createConnectedCouple();
        LocalDate futureDate = LocalDate.now(ServiceTime.ZONE_ID).plusDays(2);
        MvcResult createResult = mockMvc.perform(post("/api/v1/date-courses")
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"데이트",
                                  "date":"%s",
                                  "time":"18:00:00"
                                }
                                """.formatted(futureDate)))
                .andExpect(status().isCreated())
                .andReturn();
        Long dateCourseId = readLongFromJson(createResult, "$.dateCourseId");
        Place outsider = placeRepository.save(place("outsider", Instant.now()));

        mockMvc.perform(put("/api/v1/date-courses/{dateCourseId}", dateCourseId)
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":0,
                                  "title":"데이트",
                                  "date":"%s",
                                  "time":"18:00:00",
                                  "placeIds":[%d]
                                }
                                """.formatted(futureDate, outsider.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DATE_COURSE_PLACE_NOT_SAVED"));
    }

    @Test
    void rejectsStaleVersionOnSave() throws Exception {
        CoupleFixture fixture = createConnectedCouple();
        Place first = savePlaceForMember(fixture.first().member().getId(), "first", Instant.now());
        LocalDate futureDate = LocalDate.now(ServiceTime.ZONE_ID).plusDays(4);
        MvcResult createResult = mockMvc.perform(post("/api/v1/date-courses")
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"버전 테스트",
                                  "date":"%s",
                                  "time":"20:00:00"
                                }
                                """.formatted(futureDate)))
                .andExpect(status().isCreated())
                .andReturn();
        Long dateCourseId = readLongFromJson(createResult, "$.dateCourseId");

        mockMvc.perform(put("/api/v1/date-courses/{dateCourseId}", dateCourseId)
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":0,
                                  "title":"버전 테스트",
                                  "date":"%s",
                                  "time":"20:00:00",
                                  "placeIds":[%d]
                                }
                                """.formatted(futureDate, first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/v1/date-courses/{dateCourseId}", dateCourseId)
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":0,
                                  "title":"버전 테스트",
                                  "date":"%s",
                                  "time":"20:00:00",
                                  "placeIds":[%d]
                                }
                                """.formatted(futureDate, first.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATE_COURSE_CONFLICT"));
    }

    @Test
    void readsPastDatesFromHomeAndDateCourseEndpoints() throws Exception {
        CoupleFixture fixture = createConnectedCouple();
        Place first = savePlaceForMember(fixture.first().member().getId(), "first", Instant.now());
        LocalDate pastDate = LocalDate.now(ServiceTime.ZONE_ID).minusDays(2);
        MvcResult createResult = mockMvc.perform(post("/api/v1/date-courses")
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"지난 데이트",
                                  "date":"%s",
                                  "time":"10:00:00"
                                }
                                """.formatted(pastDate)))
                .andExpect(status().isCreated())
                .andReturn();
        Long dateCourseId = readLongFromJson(createResult, "$.dateCourseId");

        mockMvc.perform(put("/api/v1/date-courses/{dateCourseId}", dateCourseId)
                        .header("Authorization", bearer(fixture.first()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":0,
                                  "title":"지난 데이트",
                                  "date":"%s",
                                  "time":"10:00:00",
                                  "placeIds":[%d]
                                }
                                """.formatted(pastDate, first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/v1/date-courses/past")
                        .header("Authorization", bearer(fixture.first()))
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.dateCourses[0].dateCourseId").value(dateCourseId));

        mockMvc.perform(get("/api/v1/home/past-dates")
                        .header("Authorization", bearer(fixture.first()))
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dateCourseId").value(dateCourseId));
    }

    private CoupleFixture createConnectedCouple() {
        TestMember first = createProfileMember("첫번째", 1);
        TestMember second = createProfileMember("두번째", 2);
        coupleCommandService.connect(
                first.member().getId(),
                new ConnectCoupleCommand(second.connectionCode())
        );
        return new CoupleFixture(first, second);
    }

    private TestMember createProfileMember(String nickname, int profileIcon) {
        String subject = "date-course-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        IssuedTokens tokens = tokenService.issue(member);
        String connectionCode = memberCommandService.initializeProfile(
                member.getId(),
                new InitializeMemberProfileCommand(nickname, profileIcon, PREFERENCES)
        ).connectionCode().code();
        return new TestMember(member, tokens, connectionCode);
    }

    private Place savePlaceForMember(Long memberId, String suffix, Instant now) {
        Place place = placeRepository.save(place(suffix, now));
        memberPlaceRepository.save(MemberPlace.save(memberId, place, null, null, now));
        return place;
    }

    private Place place(String suffix, Instant now) {
        return Place.create(
                "kakao-" + suffix + "-" + UUID.randomUUID(),
                "테스트 장소 " + suffix,
                "서울 성동구 성수동",
                "서울 성동구 연무장길 1",
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0560000"),
                "음식점 > 카페",
                "CE7",
                null,
                now
        );
    }

    private String bearer(TestMember member) {
        return "Bearer " + member.tokens().accessToken();
    }

    private Long readLongFromJson(MvcResult result, String jsonPath) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
        return value.longValue();
    }

    private record CoupleFixture(TestMember first, TestMember second) {
    }

    private record TestMember(Member member, IssuedTokens tokens, String connectionCode) {
    }
}
