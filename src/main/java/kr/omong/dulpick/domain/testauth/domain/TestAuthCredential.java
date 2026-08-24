package kr.omong.dulpick.domain.testauth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;

import java.time.Instant;

@Entity
@Table(name = "test_auth_credentials")
public class TestAuthCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TestAuthCredential() {
    }

    private TestAuthCredential(
            Member member,
            String email,
            String passwordHash,
            Instant createdAt
    ) {
        this.member = member;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public static TestAuthCredential create(
            Member member,
            String email,
            String passwordHash,
            Instant createdAt
    ) {
        return new TestAuthCredential(member, email, passwordHash, createdAt);
    }

    public Member getMember() {
        return member;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void reassignMember(Member member) {
        this.member = member;
    }
}
