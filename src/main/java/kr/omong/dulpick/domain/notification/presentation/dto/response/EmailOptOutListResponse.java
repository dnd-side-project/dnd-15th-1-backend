package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.domain.EmailOptOut;

import java.time.Instant;
import java.util.List;

@Schema(description = "이메일 공지 수신 거부 목록")
public record EmailOptOutListResponse(
        List<Item> optOuts
) {

    public record Item(
            @Schema(example = "101") Long memberId,
            @Schema(example = "POLICY") String category,
            @Schema(example = "2026-08-26T12:00:00Z") Instant createdAt
    ) {
    }

    public static EmailOptOutListResponse from(List<EmailOptOut> optOuts) {
        return new EmailOptOutListResponse(optOuts.stream()
                .map(optOut -> new Item(optOut.getMemberId(), optOut.getCategory(), optOut.getCreatedAt()))
                .toList());
    }
}
