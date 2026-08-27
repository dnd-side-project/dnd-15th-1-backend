package kr.omong.dulpick.domain.couple.application.query;

import kr.omong.dulpick.domain.couple.application.query.reader.CoupleConnectionReader;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoupleQueryService {

    private final CoupleConnectionReader coupleConnectionReader;

    public CoupleQueryService(CoupleConnectionReader coupleConnectionReader) {
        this.coupleConnectionReader = coupleConnectionReader;
    }

    @Transactional(readOnly = true)
    public CoupleConnectionStatus getMyStatus(Long memberId) {
        return coupleConnectionReader.read(memberId);
    }

}
