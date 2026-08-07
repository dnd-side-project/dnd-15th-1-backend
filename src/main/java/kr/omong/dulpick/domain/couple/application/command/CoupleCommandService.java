package kr.omong.dulpick.domain.couple.application.command;

import kr.omong.dulpick.domain.couple.application.command.handler.ConnectCoupleHandler;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import org.springframework.stereotype.Service;

@Service
public class CoupleCommandService {

    private final ConnectCoupleHandler connectCoupleHandler;

    public CoupleCommandService(ConnectCoupleHandler connectCoupleHandler) {
        this.connectCoupleHandler = connectCoupleHandler;
    }

    public CoupleConnectionStatus connect(Long memberId, ConnectCoupleCommand command) {
        return connectCoupleHandler.handle(memberId, command);
    }
}
