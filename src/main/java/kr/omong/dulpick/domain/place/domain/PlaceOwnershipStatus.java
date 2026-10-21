package kr.omong.dulpick.domain.place.domain;

public enum PlaceOwnershipStatus {
    MINE,
    PARTNER,
    TOGETHER;

    public static PlaceOwnershipStatus resolve(boolean savedByMe, boolean savedByPartner) {
        if (savedByMe && savedByPartner) {
            return TOGETHER;
        }
        if (savedByMe) {
            return MINE;
        }
        if (savedByPartner) {
            return PARTNER;
        }
        return null;
    }

    public static boolean matchesFilter(
            PlaceOwnershipStatus filter,
            boolean savedByMe,
            boolean savedByPartner
    ) {
        if (filter == null) {
            return true;
        }
        return switch (filter) {
            case MINE -> savedByMe && !savedByPartner;
            case PARTNER -> savedByPartner && !savedByMe;
            // TOGETHER 필터는 커플이 저장한 장소 전체(합집합)를 조회합니다.
            case TOGETHER -> savedByMe || savedByPartner;
        };
    }
}
