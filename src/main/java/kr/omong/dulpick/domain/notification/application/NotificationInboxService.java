package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.notification.domain.Notification;
import kr.omong.dulpick.domain.notification.domain.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class NotificationInboxService {

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationCursorCodec cursorCodec;
    private final Clock clock;

    public NotificationInboxService(
            MemberRepository memberRepository,
            NotificationRepository notificationRepository,
            NotificationCursorCodec cursorCodec,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.cursorCodec = cursorCodec;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationPageView getPage(Long memberId, String cursor, int size) {
        validateActiveMember(memberId);
        List<Notification> result = notificationRepository.findPage(
                memberId,
                cursorCodec.decode(cursor),
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = result.size() > size;
        List<Notification> page = hasNext ? result.subList(0, size) : result;
        return new NotificationPageView(
                page.stream().map(this::toView).toList(),
                nextCursor(page, hasNext),
                hasNext,
                notificationRepository.countUnreadByMemberId(memberId)
        );
    }

    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        validateActiveMember(memberId);
        Notification notification = notificationRepository.findOwnedForUpdate(
                notificationId,
                memberId
        ).orElseThrow(NotificationNotFoundException::new);
        notification.markRead(clock.instant());
    }

    @Transactional
    public void markAllRead(Long memberId) {
        validateActiveMember(memberId);
        notificationRepository.markAllRead(memberId, clock.instant());
    }

    private void validateActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
    }

    private String nextCursor(List<Notification> page, boolean hasNext) {
        if (!hasNext || page.isEmpty()) {
            return null;
        }
        return cursorCodec.encode(page.getLast().getId());
    }

    private NotificationView toView(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getRoute(),
                notification.getReferenceId(),
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
