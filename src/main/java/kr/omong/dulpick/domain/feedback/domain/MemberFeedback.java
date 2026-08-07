package kr.omong.dulpick.domain.feedback.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.global.exception.FieldValidationException;
import kr.omong.dulpick.global.exception.ErrorCode;

import java.time.Instant;

@Entity
@Table(name = "member_feedbacks")
public class MemberFeedback {

    private static final int MAX_CONTENT_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "client_request_id", nullable = false, length = 36)
    private String clientRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeedbackType type;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MemberFeedback() {
    }

    private MemberFeedback(
            Member member,
            String clientRequestId,
            FeedbackType type,
            String content,
            Instant createdAt
    ) {
        this.member = member;
        this.clientRequestId = clientRequestId;
        this.type = requireType(type);
        this.content = normalizeContent(content);
        this.status = FeedbackStatus.RECEIVED;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static MemberFeedback receive(
            Member member,
            String clientRequestId,
            FeedbackType type,
            String content,
            Instant createdAt
    ) {
        if (member == null || clientRequestId == null || createdAt == null) {
            throw invalid("feedback", "REQUIRED", "피드백 정보를 입력해야 합니다");
        }
        return new MemberFeedback(member, clientRequestId, type, content, createdAt);
    }

    public Long getId() {
        return id;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static FeedbackType requireType(FeedbackType type) {
        if (type == null) {
            throw invalid("type", "REQUIRED", "피드백 유형을 입력해야 합니다");
        }
        return type;
    }

    private static String normalizeContent(String content) {
        if (content == null) {
            throw invalid("content", "REQUIRED", "피드백 내용을 입력해야 합니다");
        }
        String normalized = content.strip();
        if (normalized.isBlank() || normalized.length() > MAX_CONTENT_LENGTH) {
            throw invalid(
                    "content",
                    "INVALID_LENGTH",
                    "피드백 내용은 1~1,000자여야 합니다"
            );
        }
        if (normalized.chars().anyMatch(MemberFeedback::isForbiddenControl)) {
            throw invalid("content", "INVALID_CHARACTER", "허용되지 않는 문자가 포함되어 있습니다");
        }
        return normalized;
    }

    private static boolean isForbiddenControl(int character) {
        return Character.isISOControl(character)
                && character != '\n'
                && character != '\r'
                && character != '\t';
    }

    private static FieldValidationException invalid(
            String field,
            String reason,
            String message
    ) {
        return new FieldValidationException(
                ErrorCode.INVALID_INPUT,
                field,
                reason,
                message
        );
    }
}
