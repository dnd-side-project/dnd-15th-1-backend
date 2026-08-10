package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PublicContentNotFoundException;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PublicContentQueryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final ContentRepository contentRepository;
    private final ContentPlaceRepository contentPlaceRepository;
    private final PlaceRepository placeRepository;
    private final MemberPlaceRepository memberPlaceRepository;

    public PublicContentQueryService(
            ContentRepository contentRepository,
            ContentPlaceRepository contentPlaceRepository,
            PlaceRepository placeRepository,
            MemberPlaceRepository memberPlaceRepository
    ) {
        this.contentRepository = contentRepository;
        this.contentPlaceRepository = contentPlaceRepository;
        this.placeRepository = placeRepository;
        this.memberPlaceRepository = memberPlaceRepository;
    }

    @Transactional(readOnly = true)
    public Page<PublicContentView> findPublicContents(Long memberId, Pageable pageable) {
        Pageable orderedPageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                LATEST_FIRST
        );
        Page<Content> contents = contentRepository.findAllByPublicationStatus(
                ContentPublicationStatus.PUBLIC,
                orderedPageable
        );
        List<Long> contentIds = contents.getContent().stream().map(Content::getId).toList();
        Map<Long, List<Place>> placesByContent = findPlacesByContent(contentIds);
        Set<Long> savedPlaceIds = findSavedPlaceIds(memberId, placesByContent);
        return contents.map(content -> toView(content, placesByContent, savedPlaceIds));
    }

    @Transactional(readOnly = true)
    public PublicContentView findPublicContent(Long memberId, Long contentId) {
        Content content = contentRepository.findByIdAndPublicationStatus(
                        contentId,
                        ContentPublicationStatus.PUBLIC
                )
                .orElseThrow(PublicContentNotFoundException::new);
        Map<Long, List<Place>> placesByContent = findPlacesByContent(List.of(contentId));
        return toView(content, placesByContent, findSavedPlaceIds(memberId, placesByContent));
    }

    private Map<Long, List<Place>> findPlacesByContent(List<Long> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
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
        return placeIdsByContent.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                        .map(places::get)
                        .filter(java.util.Objects::nonNull)
                        .toList()
        ));
    }

    private PublicContentView toView(
            Content content,
            Map<Long, List<Place>> placesByContent,
            Set<Long> savedPlaceIds
    ) {
        List<PublicPlaceView> places = placesByContent.getOrDefault(content.getId(), List.of()).stream()
                .map(place -> toPlaceView(place, savedPlaceIds.contains(place.getId())))
                .toList();
        return new PublicContentView(
                content.getId(),
                content.getCanonicalUrl(),
                content.getSourceType(),
                author(content),
                content.getSourcePublishedOn(),
                engagement(content),
                content.getTitle(),
                content.getContent(),
                content.getThumbnailUrl(),
                content.getPlaceCount(),
                places
        );
    }

    private PublicPlaceView toPlaceView(Place place, boolean savedByMe) {
        return new PublicPlaceView(
                place.getId(),
                place.getKakaoPlaceId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory(),
                place.getCategoryName(),
                savedByMe,
                place.getThumbnailUrl(),
                place.getImageUrls()
        );
    }

    private Set<Long> findSavedPlaceIds(
            Long memberId,
            Map<Long, List<Place>> placesByContent
    ) {
        List<Long> placeIds = placesByContent.values().stream()
                .flatMap(List::stream)
                .map(Place::getId)
                .distinct()
                .toList();
        if (placeIds.isEmpty()) {
            return Set.of();
        }
        return memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(memberId, placeIds)
                .stream()
                .map(MemberPlace::getPlace)
                .map(Place::getId)
                .collect(Collectors.toSet());
    }

    private PublicContentView.ContentAuthorView author(Content content) {
        if (content.getSourceAuthorName() == null && content.getSourceAuthorUsername() == null) {
            return null;
        }
        return new PublicContentView.ContentAuthorView(
                content.getSourceAuthorName(),
                content.getSourceAuthorUsername()
        );
    }

    private PublicContentView.ContentEngagementView engagement(Content content) {
        if (content.getLikeCount() == null
                && content.getCommentCount() == null
                && content.getEngagementCheckedAt() == null) {
            return null;
        }
        return new PublicContentView.ContentEngagementView(
                content.getLikeCount(),
                content.getCommentCount(),
                content.getEngagementCheckedAt()
        );
    }
}
