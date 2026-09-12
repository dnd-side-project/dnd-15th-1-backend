package kr.omong.dulpick.domain.notice.application;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.notice.domain.Notice;
import kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaign;
import kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaignRepository;
import kr.omong.dulpick.domain.notice.domain.NoticeRepository;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class NoticeService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int PUSH_BODY_LIMIT = 500;

    private final NoticeRepository noticeRepository;
    private final NoticeNotificationCampaignRepository campaignRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    public NoticeService(
            NoticeRepository noticeRepository,
            NoticeNotificationCampaignRepository campaignRepository,
            MemberRepository memberRepository,
            Clock clock
    ) {
        this.noticeRepository = noticeRepository;
        this.campaignRepository = campaignRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NoticePageView list(int page, int size) {
        return NoticePageView.from(noticeRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, size))
                .map(NoticeView::from));
    }

    @Transactional(readOnly = true)
    public NoticeView get(Long noticeId) {
        return NoticeView.from(findNotice(noticeId));
    }

    @Transactional
    public NoticeCreateView create(NoticeCommand command) {
        Instant now = now();
        Notice notice = noticeRepository.save(Notice.create(
                normalizeTitle(command.title()),
                normalizeContent(command.content()),
                now
        ));
        if (!command.sendNotification()) {
            return new NoticeCreateView(NoticeView.from(notice), null);
        }
        NoticeNotificationCampaign campaign = campaignRepository.save(
                NoticeNotificationCampaign.create(
                        UUID.randomUUID().toString(),
                        notice.getId(),
                        pushTitle(notice.getTitle()),
                        notificationBody(notice.getContent()),
                        Math.toIntExact(memberRepository.countByStatus(MemberStatus.ACTIVE)),
                        now
                )
        );
        return new NoticeCreateView(NoticeView.from(notice), NoticeNotificationView.from(campaign));
    }

    @Transactional
    public NoticeView update(Long noticeId, NoticeUpdateCommand command) {
        Notice notice = findNotice(noticeId);
        notice.update(
                normalizeTitle(command.title()),
                normalizeContent(command.content()),
                command.expectedUpdatedAt(),
                now()
        );
        return NoticeView.from(notice);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private String normalizeTitle(String title) {
        return title == null ? null : title.strip();
    }

    private String normalizeContent(String content) {
        return content == null ? null : content.strip();
    }

    private String pushTitle(String title) {
        return title.length() <= 100 ? title : title.substring(0, 97) + "...";
    }

    private String notificationBody(String content) {
        return content.length() <= PUSH_BODY_LIMIT
                ? content
                : content.substring(0, PUSH_BODY_LIMIT - 3) + "...";
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
