package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;

public record PlaceOwnership(
        PlaceOwnershipStatus status,
        boolean savedByMe,
        boolean savedByPartner
) {

    public static PlaceOwnership none() {
        return new PlaceOwnership(null, false, false);
    }

    public static PlaceOwnership of(
            boolean hasActiveCouple,
            boolean savedByMe,
            boolean savedByPartner
    ) {
        return new PlaceOwnership(
                PlaceOwnershipStatus.resolve(hasActiveCouple, savedByMe, savedByPartner),
                savedByMe,
                savedByPartner
        );
    }

    public boolean matchesFilter(PlaceOwnershipStatus filter) {
        return PlaceOwnershipStatus.matchesFilter(filter, savedByMe, savedByPartner);
    }
}
