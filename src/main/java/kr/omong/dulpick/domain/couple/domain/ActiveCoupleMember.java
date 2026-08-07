package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;

import java.time.Instant;

@Entity
@Table(name = "active_couple_members")
public class ActiveCoupleMember {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couple_id", nullable = false)
    private Couple couple;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected ActiveCoupleMember() {
    }

    private ActiveCoupleMember(Member member, Couple couple, Instant joinedAt) {
        this.member = member;
        this.couple = couple;
        this.joinedAt = joinedAt;
    }

    public static ActiveCoupleMember join(
            Member member,
            Couple couple,
            Instant joinedAt
    ) {
        return new ActiveCoupleMember(member, couple, joinedAt);
    }

    public Long getMemberId() {
        return memberId;
    }

    public Member getMember() {
        return member;
    }

    public Couple getCouple() {
        return couple;
    }
}
