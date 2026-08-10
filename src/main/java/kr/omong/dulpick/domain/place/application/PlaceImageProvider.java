package kr.omong.dulpick.domain.place.application;

import java.util.List;

public interface PlaceImageProvider {

    List<String> findImageUrls(String externalPlaceId);
}
