package kr.omong.dulpick.domain.place.application;

public record RegionTagView(
        Long regionTagId,
        String name,
        int displayOrder,
        long placeCount
) {
}
