package kr.omong.dulpick.domain.place.application;

public record RegionTagSummaryView(
        Long regionTagId,
        String name,
        int displayOrder
) {
}
