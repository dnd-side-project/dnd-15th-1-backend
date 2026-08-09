package kr.omong.dulpick.domain.member.domain;

import kr.omong.dulpick.domain.member.domain.exception.MemberAlreadyWithdrawnException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    void recordsLatestWithdrawalAndRejoinDates() {
        Member member = Member.create(Instant.EPOCH);
        Instant firstWithdrawal = Instant.parse("2026-07-27T00:00:00Z");
        Instant firstRejoin = Instant.parse("2026-07-28T00:00:00Z");
        Instant latestWithdrawal = Instant.parse("2026-07-29T00:00:00Z");
        Instant latestRejoin = Instant.parse("2026-07-30T00:00:00Z");

        member.withdraw(firstWithdrawal);
        member.rejoin(firstRejoin);
        member.withdraw(latestWithdrawal);
        member.rejoin(latestRejoin);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getLastWithdrawnAt()).isEqualTo(latestWithdrawal);
        assertThat(member.getLastRejoinedAt()).isEqualTo(latestRejoin);
        assertThat(member.getUpdatedAt()).isEqualTo(latestRejoin);
        assertThat(member.getTokenVersion()).isEqualTo(2);
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
