package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.admin.EmailAnnouncementAdminService;

import java.util.List;

@Schema(description = "이메일 공지 발송 대상 미리보기")
public record EmailAnnouncementPreviewResponse(
        @Schema(description = "발송 대상 이메일·닉네임 목록") List<Recipient> recipients,
        @Schema(example = "42") int total
) {

    public record Recipient(
            @Schema(example = "user@example.com") String email,
            @Schema(example = "준서") String nickname
    ) {
    }

    public static EmailAnnouncementPreviewResponse from(List<EmailAnnouncementAdminService.AnnouncementRecipient> recipients) {
        List<Recipient> rows = recipients.stream()
                .map(recipient -> new Recipient(recipient.email(), recipient.nickname()))
                .toList();
        return new EmailAnnouncementPreviewResponse(rows, rows.size());
    }
}
