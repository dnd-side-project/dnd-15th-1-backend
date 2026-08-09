package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PublicContentQueryService {

    private final ContentRepository contentRepository;
    private final ContentPlaceRepository contentPlaceRepository;
    private final PlaceRepository placeRepository;

    public PublicContentQueryService(
            ContentRepository contentRepository,
            ContentPlaceRepository contentPlaceRepository,
            PlaceRepository placeRepository
    ) {
        this.contentRepository = contentRepository;
        this.contentPlaceRepository = contentPlaceRepository;
        this.placeRepository = placeRepository;
    }

    @Transactional(readOnly = true)
    public List<PublicContentView> findPublicContents() {
        return contentRepository.findAllByPublicationStatusOrderByCreatedAtDesc(
                        ContentPublicationStatus.PUBLIC
                ).stream()
                .map(this::toView)
                .toList();
    }

    private PublicContentView toView(Content content) {
        List<Long> placeIds = contentPlaceRepository.findAllByContentId(content.getId())
                .stream()
                .map(kr.omong.dulpick.domain.place.domain.ContentPlace::getPlaceId)
                .toList();
        List<MemberPlaceView> places = placeRepository.findAllById(placeIds).stream()
                .map(this::toPlaceView)
                .toList();
        return new PublicContentView(
                content.getId(),
                content.getCanonicalUrl(),
                content.getSourceType(),
                content.getTitle(),
                content.getContent(),
                content.getThumbnailUrl(),
                content.getPublicationStatus(),
                places
        );
    }

    private MemberPlaceView toPlaceView(Place place) {
        return new MemberPlaceView(
                null,
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory(),
                null,
                null,
                null
        );
    }
}
