package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.domain.EmailAnnouncement;

import java.time.Instant;

@Schema(description = "이메일 공지 발송 처리 결과")
public record EmailAnnouncementSendResponse(
        @Schema(example = "8f4d1f6f-4d9b-4f5e-9c53-7d2c0e4b7a11") String announcementId,
        @Schema(example = "COMPLETED_LOG_ONLY") String status,
        @Schema(example = "42") int targetCount,
        @Schema(example = "LOG_ONLY") String deliveryMode,
        @Schema(example = "2026-08-26T12:00:00Z") Instant createdAt
) {

    public static EmailAnnouncementSendResponse from(EmailAnnouncement announcement) {
        return new EmailAnnouncementSendResponse(
                announcement.getId(),
                announcement.getStatus(),
                announcement.getTargetCount(),
                announcement.getDeliveryMode(),
                announcement.getCreatedAt()
        );
    }
}
