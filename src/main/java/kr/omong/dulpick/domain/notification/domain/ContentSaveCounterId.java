package kr.omong.dulpick.domain.notification.domain;

import java.io.Serializable;
import java.util.Objects;

public class ContentSaveCounterId implements Serializable {

    private Long coupleId;
    private Long saverMemberId;

    protected ContentSaveCounterId() {
    }

    public ContentSaveCounterId(Long coupleId, Long saverMemberId) {
        this.coupleId = coupleId;
        this.saverMemberId = saverMemberId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ContentSaveCounterId other)) {
            return false;
        }
        return Objects.equals(coupleId, other.coupleId)
                && Objects.equals(saverMemberId, other.saverMemberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coupleId, saverMemberId);
    }
}
