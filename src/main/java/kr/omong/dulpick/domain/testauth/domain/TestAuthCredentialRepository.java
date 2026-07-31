package kr.omong.dulpick.domain.testauth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestAuthCredentialRepository
        extends JpaRepository<TestAuthCredential, Long> {

    boolean existsByEmail(String email);

    boolean existsByMemberId(Long memberId);

    Optional<TestAuthCredential> findByEmail(String email);
}
