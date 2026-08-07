package kr.omong.dulpick.domain.member.application.query;

import kr.omong.dulpick.domain.member.application.query.reader.MemberProfileReader;
import kr.omong.dulpick.domain.member.application.query.view.MemberProfileView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberProfileReader memberProfileReader;

    public MemberQueryService(MemberProfileReader memberProfileReader) {
        this.memberProfileReader = memberProfileReader;
    }

    public MemberProfileView getMyProfile(Long memberId) {
        return memberProfileReader.read(memberId);
    }
}
