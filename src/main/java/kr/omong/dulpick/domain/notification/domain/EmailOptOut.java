package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "email_opt_outs")
public class EmailOptOut {

    public static final String CATEGORY_POLICY = "POLICY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String category;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailOptOut() {
    }

    public static EmailOptOut create(Long memberId, String category, Instant now) {
        EmailOptOut optOut = new EmailOptOut();
        optOut.memberId = memberId;
        optOut.category = category;
        optOut.createdAt = now;
        return optOut;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
