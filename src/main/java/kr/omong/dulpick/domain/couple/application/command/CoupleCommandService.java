package kr.omong.dulpick.domain.couple.application.command;

import kr.omong.dulpick.domain.couple.application.command.handler.ConnectCoupleHandler;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.application.support.CoupleDisconnectionService;
import kr.omong.dulpick.domain.couple.application.support.ConnectionAbusePreventionService;
import kr.omong.dulpick.domain.couple.application.support.ConnectionAbusePreventionService.AttemptPermit;
import kr.omong.dulpick.domain.couple.domain.ConnectionAttempt;
import kr.omong.dulpick.global.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class CoupleCommandService {

    private final ConnectCoupleHandler connectCoupleHandler;
    private final CoupleDisconnectionService coupleDisconnectionService;
    private final ConnectionAbusePreventionService abusePreventionService;

    public CoupleCommandService(
            ConnectCoupleHandler connectCoupleHandler,
            CoupleDisconnectionService coupleDisconnectionService,
            ConnectionAbusePreventionService abusePreventionService
    ) {
        this.connectCoupleHandler = connectCoupleHandler;
        this.coupleDisconnectionService = coupleDisconnectionService;
        this.abusePreventionService = abusePreventionService;
    }

    public CoupleConnectionStatus connect(Long memberId, ConnectCoupleCommand command) {
        return connect(memberId, command, "internal");
    }

    public CoupleConnectionStatus connect(
            Long memberId,
            ConnectCoupleCommand command,
            String clientAddress
    ) {
        AttemptPermit permit = begin(memberId, clientAddress, ConnectionAttempt.Action.CONNECT);
        try {
            CoupleConnectionStatus status = connectCoupleHandler.handle(memberId, command);
            abusePreventionService.completeSuccess(permit);
            return status;
        } catch (BusinessException exception) {
            abusePreventionService.completeFailure(memberId, permit, exception);
            throw exception;
        }
    }

    public void disconnect(Long memberId) {
        disconnect(memberId, "internal");
    }

    public void disconnect(Long memberId, String clientAddress) {
        AttemptPermit permit = begin(
                memberId,
                clientAddress,
                ConnectionAttempt.Action.DISCONNECT
        );
        try {
            coupleDisconnectionService.disconnectByMember(memberId);
            abusePreventionService.completeSuccess(permit);
        } catch (BusinessException exception) {
            abusePreventionService.completeFailure(memberId, permit, exception);
            throw exception;
        }
    }

    private AttemptPermit begin(
            Long memberId,
            String clientAddress,
            ConnectionAttempt.Action action
    ) {
        AttemptPermit permit = abusePreventionService.begin(memberId, clientAddress, action);
        permit.requireAllowed();
        return permit;
    }
}
