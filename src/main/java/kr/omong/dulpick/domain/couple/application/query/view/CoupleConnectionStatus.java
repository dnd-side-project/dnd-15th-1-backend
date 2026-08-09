package kr.omong.dulpick.domain.couple.application.query.view;

import java.time.Instant;

public record CoupleConnectionStatus(
        boolean connected,
        CoupleMemberProfile me,
        CoupleMemberProfile partner,
        Instant connectedAt,
        Long daysTogether
) {

    public static CoupleConnectionStatus disconnected(CoupleMemberProfile me) {
        return new CoupleConnectionStatus(false, me, null, null, null);
    }

    public static CoupleConnectionStatus connected(
            CoupleMemberProfile me,
            CoupleMemberProfile partner,
            Instant connectedAt,
            long daysTogether
    ) {
        return new CoupleConnectionStatus(
                true,
                me,
                partner,
                connectedAt,
                daysTogether
        );
    }
}
