package kr.omong.dulpick.domain.notice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

import java.time.Instant;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Notice() {
    }

    private Notice(String title, String content, Instant createdAt) {
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Notice create(String title, String content, Instant createdAt) {
        if (title == null || title.isBlank() || title.length() > 200
                || content == null || content.isBlank() || content.length() > 10_000
                || createdAt == null) {
            throw new IllegalArgumentException("Notice is invalid");
        }
        return new Notice(title, content, createdAt);
    }

    public void update(String title, String content, Instant expectedUpdatedAt, Instant updatedAt) {
        ensureFresh(expectedUpdatedAt);
        if (title == null || title.isBlank() || title.length() > 200
                || content == null || content.isBlank() || content.length() > 10_000
                || updatedAt == null) {
            throw new IllegalArgumentException("Notice is invalid");
        }
        this.title = title;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    private void ensureFresh(Instant expectedUpdatedAt) {
        if (expectedUpdatedAt == null || !updatedAt.equals(expectedUpdatedAt)) {
            throw new BusinessException(ErrorCode.ADMIN_RESOURCE_MODIFIED);
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
