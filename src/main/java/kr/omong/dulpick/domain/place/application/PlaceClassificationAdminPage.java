package kr.omong.dulpick.domain.place.application;

import java.util.List;

public record PlaceClassificationAdminPage(
        List<PlaceClassificationAdminView> places,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        StatusCounts counts
) {

    public record StatusCounts(
            long all,
            long unclassified,
            long partiallyClassified,
            long classified
    ) {
    }
}
