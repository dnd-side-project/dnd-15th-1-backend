package kr.omong.dulpick.domain.member.domain;

import kr.omong.dulpick.domain.member.domain.exception.MemberAlreadyWithdrawnException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    void recordsWithdrawalAndInvalidatesTokenVersion() {
        Member member = Member.create(Instant.EPOCH);
        Instant withdrawnAt = Instant.parse("2026-07-29T00:00:00Z");

        member.withdraw(withdrawnAt);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getLastWithdrawnAt()).isEqualTo(withdrawnAt);
        assertThat(member.getUpdatedAt()).isEqualTo(withdrawnAt);
        assertThat(member.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateWithdrawalInDomain() {
        Member member = Member.create(Instant.EPOCH);
        member.withdraw(Instant.parse("2026-07-27T00:00:00Z"));

        assertThatThrownBy(() -> member.withdraw(
                Instant.parse("2026-07-28T00:00:00Z")
        )).isInstanceOf(MemberAlreadyWithdrawnException.class);
    }
}
