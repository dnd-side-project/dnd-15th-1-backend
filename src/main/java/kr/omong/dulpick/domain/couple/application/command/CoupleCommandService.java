package kr.omong.dulpick.domain.couple.application.command;

import kr.omong.dulpick.domain.couple.application.command.handler.ConnectCoupleHandler;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.application.support.CoupleDisconnectionService;
import org.springframework.stereotype.Service;

@Service
public class CoupleCommandService {

    private final ConnectCoupleHandler connectCoupleHandler;
    private final CoupleDisconnectionService coupleDisconnectionService;

    public CoupleCommandService(
            ConnectCoupleHandler connectCoupleHandler,
            CoupleDisconnectionService coupleDisconnectionService
    ) {
        this.connectCoupleHandler = connectCoupleHandler;
        this.coupleDisconnectionService = coupleDisconnectionService;
    }

    public CoupleConnectionStatus connect(Long memberId, ConnectCoupleCommand command) {
        return connectCoupleHandler.handle(memberId, command);
    }

    public void disconnect(Long memberId) {
        coupleDisconnectionService.disconnectByMember(memberId);
    }
}
