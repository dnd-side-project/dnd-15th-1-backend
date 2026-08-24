package kr.omong.dulpick.domain.notification.application.admin;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaign;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaignRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class MarketingNotificationAdminService {

    private final MemberNotificationSettingsRepository settingsRepository;
    private final MarketingNotificationCampaignRepository campaignRepository;
    private final Clock clock;

    public MarketingNotificationAdminService(
            MemberNotificationSettingsRepository settingsRepository,
            MarketingNotificationCampaignRepository campaignRepository,
            Clock clock
    ) {
        this.settingsRepository = settingsRepository;
        this.campaignRepository = campaignRepository;
        this.clock = clock;
    }

    @Transactional
    public MarketingNotificationSendView send(MarketingNotificationCommand command) {
        Instant queuedAt = clock.instant();
        String campaignId = UUID.randomUUID().toString();
        MarketingNotificationCampaign campaign = campaignRepository.save(
                MarketingNotificationCampaign.create(
                        campaignId,
                        command.title(),
                        command.body(),
                        Math.toIntExact(settingsRepository.countMembersWithMarketingEnabled(
                                MemberStatus.ACTIVE
                        )),
                        queuedAt
                )
        );
        return MarketingNotificationSendView.from(campaign);
    }

    @Transactional(readOnly = true)
    public MarketingNotificationSendView get(String campaignId) {
        return campaignRepository.findById(campaignId)
                .map(MarketingNotificationSendView::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
