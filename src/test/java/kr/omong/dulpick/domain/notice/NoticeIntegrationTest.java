package kr.omong.dulpick.domain.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.omong.dulpick.domain.notice.domain.Notice;
import kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaignRepository;
import kr.omong.dulpick.domain.notice.domain.NoticeRepository;
import kr.omong.dulpick.global.security.config.OpsAccessProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoticeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpsAccessProperties opsAccessProperties;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeNotificationCampaignRepository campaignRepository;

    @Test
    void publicClientCanListAndReadNotice() throws Exception {
        Notice notice = noticeRepository.save(Notice.create(
                "서비스 안내",
                "새로운 공지사항 내용입니다.",
                Instant.now()
        ));

        mockMvc.perform(get("/api/v1/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices[0].noticeId").value(notice.getId()))
                .andExpect(jsonPath("$.notices[0].title").value("서비스 안내"))
                .andExpect(jsonPath("$.notices[0].content").value("새로운 공지사항 내용입니다."));

        mockMvc.perform(get("/api/v1/notices/{noticeId}", notice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId").value(notice.getId()))
                .andExpect(jsonPath("$.content").value("새로운 공지사항 내용입니다."));
    }

    @Test
    void operatorCanCreateSilentlyAndUpdateWithoutSendingAnotherNotification() throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/notices")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"운영 공지","content":"작성 내용","sendNotification":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notice.title").value("운영 공지"))
                .andExpect(jsonPath("$.notification").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var created = objectMapper.readTree(response);
        long noticeId = created.path("notice").path("noticeId").asLong();
        String updatedAt = created.path("notice").path("updatedAt").asText();

        mockMvc.perform(patch("/api/v1/admin/notices/{noticeId}", noticeId)
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"운영 공지 수정","content":"잠수함패치 내용","expectedUpdatedAt":"%s"}
                                """.formatted(updatedAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("운영 공지 수정"))
                .andExpect(jsonPath("$.content").value("잠수함패치 내용"));

        assertThat(campaignRepository.count()).isZero();
    }

    @Test
    void operatorCanQueueNotificationAndCsrfIsRequired() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notices")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"알림 공지","content":"전체 회원 알림 내용","sendNotification":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notification.status").value("PENDING"))
                .andExpect(jsonPath("$.notification.targetCount").isNumber());

        assertThat(campaignRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/admin/notices")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"차단","content":"CSRF 없음","sendNotification":false}
                                """))
                .andExpect(status().isForbidden());
    }
}
