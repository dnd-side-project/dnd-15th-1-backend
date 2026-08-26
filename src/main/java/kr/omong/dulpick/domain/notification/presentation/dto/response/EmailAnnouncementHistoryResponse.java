package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.domain.EmailAnnouncement;

import java.time.Instant;
import java.util.List;

@Schema(description = "이메일 공지 발송 이력")
public record EmailAnnouncementHistoryResponse(
        List<Item> announcements,
        @Schema(example = "0") int page,
        @Schema(example = "10") int size
) {

    public record Item(
            @Schema(example = "8f4d1f6f-4d9b-4f5e-9c53-7d2c0e4b7a11") String announcementId,
            @Schema(example = "POLICY") String category,
            @Schema(example = "개인정보처리방침 변경 안내") String title,
            @Schema(example = "COMPLETED_LOG_ONLY") String status,
            @Schema(example = "42") int targetCount,
            @Schema(example = "LOG_ONLY") String deliveryMode,
            @Schema(example = "2026-08-26T12:00:00Z") Instant createdAt
    ) {
    }

    public static EmailAnnouncementHistoryResponse from(List<EmailAnnouncement> announcements, int page, int size) {
        return new EmailAnnouncementHistoryResponse(
                announcements.stream()
                        .map(announcement -> new Item(
                                announcement.getId(),
                                announcement.getCategory(),
                                announcement.getTitle(),
                                announcement.getStatus(),
                                announcement.getTargetCount(),
                                announcement.getDeliveryMode(),
                                announcement.getCreatedAt()
                        ))
                        .toList(),
                page,
                size
        );
    }
}
