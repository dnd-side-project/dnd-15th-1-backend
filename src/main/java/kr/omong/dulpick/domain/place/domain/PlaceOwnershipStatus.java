package kr.omong.dulpick.domain.place.domain;

public enum PlaceOwnershipStatus {
    MINE,
    PARTNER,
    TOGETHER;

    public static PlaceOwnershipStatus resolve(
            boolean hasActiveCouple,
            boolean savedByMe,
            boolean savedByPartner
    ) {
        if (savedByMe || savedByPartner) {
            if (hasActiveCouple) {
                return TOGETHER;
            }
            if (savedByMe) {
                return MINE;
            }
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
            case TOGETHER -> savedByMe || savedByPartner;
        };
    }
}
