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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
    public Page<PublicContentView> findPublicContents(Pageable pageable) {
        Pageable orderedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                );
        Page<Content> contents = contentRepository.findAllByPublicationStatus(
                ContentPublicationStatus.PUBLIC,
                orderedPageable
        );
        List<Long> contentIds = contents.getContent().stream().map(Content::getId).toList();
        Map<Long, List<Long>> placeIdsByContent = contentPlaceRepository
                .findAllByContentIdIn(contentIds)
                .stream()
                .collect(Collectors.groupingBy(
                        kr.omong.dulpick.domain.place.domain.ContentPlace::getContentId,
                        Collectors.mapping(
                                kr.omong.dulpick.domain.place.domain.ContentPlace::getPlaceId,
                                Collectors.toList()
                        )
                ));
        List<Long> placeIds = placeIdsByContent.values().stream().flatMap(List::stream).distinct().toList();
        Map<Long, Place> places = placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        return contents.map(content -> toView(content, placeIdsByContent, places));
    }

    private PublicContentView toView(
            Content content,
            Map<Long, List<Long>> placeIdsByContent,
            Map<Long, Place> placesById
    ) {
        List<MemberPlaceView> places = placeIdsByContent.getOrDefault(content.getId(), List.of()).stream()
                .map(placesById::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toPlaceView)
                .toList();
        return new PublicContentView(
                content.getId(),
                content.getCanonicalUrl(),
                content.getSourceType(),
                content.getTitle(),
                content.getContent(),
                content.getThumbnailUrl(),
                content.getPlaceCount(),
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
