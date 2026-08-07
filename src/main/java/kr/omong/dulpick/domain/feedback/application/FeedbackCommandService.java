package kr.omong.dulpick.domain.feedback.application;

import kr.omong.dulpick.domain.feedback.domain.MemberFeedback;
import kr.omong.dulpick.domain.feedback.domain.MemberFeedbackRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class FeedbackCommandService {

    private static final int MAX_FEEDBACKS_PER_DAY = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofDays(1);

    private final MemberRepository memberRepository;
    private final MemberFeedbackRepository feedbackRepository;
    private final Clock clock;

    public FeedbackCommandService(
            MemberRepository memberRepository,
            MemberFeedbackRepository feedbackRepository,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.feedbackRepository = feedbackRepository;
        this.clock = clock;
    }

    @Transactional
    public ReceivedFeedback receive(Long memberId, FeedbackCommand command) {
        Member member = lockActiveMember(memberId);
        String clientRequestId = command.clientRequestId().toString();
        return feedbackRepository.findByMemberIdAndClientRequestId(memberId, clientRequestId)
                .map(this::toResult)
                .orElseGet(() -> create(member, clientRequestId, command));
    }

    private Member lockActiveMember(Long memberId) {
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return member;
    }

    private ReceivedFeedback create(
            Member member,
            String clientRequestId,
            FeedbackCommand command
    ) {
        Instant now = clock.instant();
        validateRateLimit(member.getId(), now);
        MemberFeedback feedback = MemberFeedback.receive(
                member,
                clientRequestId,
                command.type(),
                command.content(),
                now
        );
        return toResult(feedbackRepository.save(feedback));
    }

    private void validateRateLimit(Long memberId, Instant now) {
        long feedbackCount = feedbackRepository.countByMemberIdAndCreatedAtGreaterThanEqual(
                memberId,
                now.minus(RATE_LIMIT_WINDOW)
        );
        if (feedbackCount >= MAX_FEEDBACKS_PER_DAY) {
            throw new FeedbackRateLimitExceededException();
        }
    }

    private ReceivedFeedback toResult(MemberFeedback feedback) {
        return new ReceivedFeedback(
                feedback.getId(),
                feedback.getStatus(),
                feedback.getCreatedAt()
        );
    }
}
