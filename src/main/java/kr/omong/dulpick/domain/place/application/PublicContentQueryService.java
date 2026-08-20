package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.place.application.exception.PublicContentNotFoundException;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRecommendationSort;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.search.FullTextBooleanQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PublicContentQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ContentRepository contentRepository;
    private final ContentPlaceRepository contentPlaceRepository;
    private final PlaceRepository placeRepository;
    private final PlaceClassificationRepository placeClassificationRepository;
    private final MemberPlaceRepository memberPlaceRepository;
    private final MemberProfileRepository memberProfileRepository;

    public PublicContentQueryService(
            ContentRepository contentRepository,
            ContentPlaceRepository contentPlaceRepository,
            PlaceRepository placeRepository,
            PlaceClassificationRepository placeClassificationRepository,
            MemberPlaceRepository memberPlaceRepository,
            MemberProfileRepository memberProfileRepository
    ) {
        this.contentRepository = contentRepository;
        this.contentPlaceRepository = contentPlaceRepository;
        this.placeRepository = placeRepository;
        this.placeClassificationRepository = placeClassificationRepository;
        this.memberPlaceRepository = memberPlaceRepository;
        this.memberProfileRepository = memberProfileRepository;
    }

    @Transactional(readOnly = true)
    public Page<PublicContentView> findPublicContents(
            Long memberId,
            Pageable pageable,
            ContentRecommendationSort sort
    ) {
        List<Content> ranked = rankContents(
                contentRepository.findAllByPublicationStatusOrderByCreatedAtDesc(
                        ContentPublicationStatus.PUBLIC
                ),
                memberId,
                sort
        );
        return enrichPage(memberId, slice(ranked, pageable), sort);
    }

    @Transactional(readOnly = true)
    public Page<PublicContentView> searchPublicContents(
            Long memberId,
            String query,
            Pageable pageable
    ) {
        Page<Content> contents = contentRepository.searchByPublicationStatusAndKeyword(
                ContentPublicationStatus.PUBLIC.name(),
                FullTextBooleanQuery.from(query.strip()),
                paged(pageable)
        );
        return enrichPage(memberId, contents, ContentRecommendationSort.POPULAR);
    }

    @Transactional(readOnly = true)
    public PublicContentView findPublicContent(Long memberId, Long contentId) {
        Content content = contentRepository.findByIdAndPublicationStatus(
                        contentId,
                        ContentPublicationStatus.PUBLIC
                )
                .orElseThrow(PublicContentNotFoundException::new);
        return enrichPage(
                memberId,
                new PageImpl<>(List.of(content)),
                ContentRecommendationSort.POPULAR
        ).getContent().getFirst();
    }

    private List<Content> rankContents(
            List<Content> contents,
            Long memberId,
            ContentRecommendationSort requestedSort
    ) {
        Map<Long, List<Place>> placesByContent = findPlacesByContent(
                contents.stream().map(Content::getId).toList()
        );
        Map<Long, PlaceDateTraitsView> traitsByPlace = findTraitsByPlace(placesByContent);
        Map<Long, Long> saveCounts = countSaves(placesByContent);
        DatePreferences preferences = requestedSort == ContentRecommendationSort.PREFERENCE
                ? datePreferences(memberId)
                : null;
        ContentRecommendationSort sort = effectiveSort(requestedSort, preferences);
        return contents.stream()
                .sorted(contentComparator(sort, preferences, placesByContent, traitsByPlace, saveCounts))
                .toList();
    }

    private Page<PublicContentView> enrichPage(
            Long memberId,
            Page<Content> contents,
            ContentRecommendationSort requestedSort
    ) {
        List<Long> contentIds = contents.getContent().stream().map(Content::getId).toList();
        Map<Long, List<Place>> placesByContent = findPlacesByContent(contentIds);
        Map<Long, PlaceDateTraitsView> traitsByPlace = findTraitsByPlace(placesByContent);
        Map<Long, Long> saveCounts = countSaves(placesByContent);
        Set<Long> savedPlaceIds = findSavedPlaceIds(memberId, placesByContent);
        DatePreferences preferences = requestedSort == ContentRecommendationSort.PREFERENCE
                ? datePreferences(memberId)
                : null;
        ContentRecommendationSort sort = effectiveSort(requestedSort, preferences);
        return contents.map(content -> toView(
                content,
                placesByContent,
                traitsByPlace,
                savedPlaceIds,
                saveCounts,
                sort,
                preferences
        ));
    }

    private Pageable paged(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)
        );
    }

    private Page<Content> slice(List<Content> ranked, Pageable pageable) {
        Pageable bounded = paged(pageable);
        int start = Math.toIntExact(bounded.getOffset());
        if (start >= ranked.size()) {
            return new PageImpl<>(List.of(), bounded, ranked.size());
        }
        int end = Math.min(start + bounded.getPageSize(), ranked.size());
        return new PageImpl<>(ranked.subList(start, end), bounded, ranked.size());
    }

    private ContentRecommendationSort effectiveSort(
            ContentRecommendationSort requestedSort,
            DatePreferences preferences
    ) {
        if (requestedSort == ContentRecommendationSort.PREFERENCE && preferences != null) {
            return ContentRecommendationSort.PREFERENCE;
        }
        return ContentRecommendationSort.POPULAR;
    }

    private DatePreferences datePreferences(Long memberId) {
        return memberProfileRepository.findById(memberId)
                .map(MemberProfile::getDatePreferences)
                .orElse(null);
    }

    private Comparator<Content> contentComparator(
            ContentRecommendationSort sort,
            DatePreferences preferences,
            Map<Long, List<Place>> placesByContent,
            Map<Long, PlaceDateTraitsView> traitsByPlace,
            Map<Long, Long> saveCounts
    ) {
        Comparator<Content> bySaves = Comparator.comparingLong(
                (Content content) -> saveCount(placesByContent.getOrDefault(content.getId(), List.of()), saveCounts)
        ).reversed();
        Comparator<Content> byId = Comparator.comparing(Content::getId).reversed();
        if (sort == ContentRecommendationSort.PREFERENCE) {
            return Comparator.comparingInt(
                            (Content content) -> preferenceScore(
                                    placesByContent.getOrDefault(content.getId(), List.of()),
                                    traitsByPlace,
                                    preferences
                            )
                    )
                    .reversed()
                    .thenComparing(bySaves)
                    .thenComparing(byId);
        }
        return bySaves.thenComparing(byId);
    }

    private int preferenceScore(
            List<Place> places,
            Map<Long, PlaceDateTraitsView> traitsByPlace,
            DatePreferences preferences
    ) {
        return places.stream()
                .mapToInt(place -> PlaceDateTraitMatcher.score(
                        preferences,
                        traitsByPlace.getOrDefault(place.getId(), PlaceDateTraitsView.unclassified())
                ))
                .max()
                .orElse(0);
    }

    private long saveCount(List<Place> places, Map<Long, Long> saveCounts) {
        return places.stream()
                .mapToLong(place -> saveCounts.getOrDefault(place.getId(), 0L))
                .sum();
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
            Map<Long, PlaceDateTraitsView> traitsByPlace,
            Set<Long> savedPlaceIds,
            Map<Long, Long> saveCounts,
            ContentRecommendationSort sort,
            DatePreferences preferences
    ) {
        List<Place> places = rankedPlaces(
                placesByContent.getOrDefault(content.getId(), List.of()),
                traitsByPlace,
                saveCounts,
                sort,
                preferences
        );
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
                places.stream()
                        .map(place -> toPlaceView(
                                place,
                                savedPlaceIds.contains(place.getId()),
                                traitsByPlace.getOrDefault(place.getId(), PlaceDateTraitsView.unclassified())
                        ))
                        .toList()
        );
    }

    private List<Place> rankedPlaces(
            List<Place> places,
            Map<Long, PlaceDateTraitsView> traitsByPlace,
            Map<Long, Long> saveCounts,
            ContentRecommendationSort sort,
            DatePreferences preferences
    ) {
        Comparator<Place> bySaves = Comparator.comparingLong(
                (Place place) -> saveCounts.getOrDefault(place.getId(), 0L)
        ).reversed();
        Comparator<Place> byId = Comparator.comparing(Place::getId).reversed();
        Comparator<Place> comparator = sort == ContentRecommendationSort.PREFERENCE
                ? Comparator.comparingInt(
                                (Place place) -> PlaceDateTraitMatcher.score(
                                        preferences,
                                        traitsByPlace.getOrDefault(place.getId(), PlaceDateTraitsView.unclassified())
                                )
                        )
                        .reversed()
                        .thenComparing(bySaves)
                        .thenComparing(byId)
                : bySaves.thenComparing(byId);
        return places.stream().sorted(comparator).toList();
    }

    private PublicPlaceView toPlaceView(
            Place place,
            boolean savedByMe,
            PlaceDateTraitsView dateTraits
    ) {
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
                place.getImageUrls(),
                dateTraits
        );
    }

    private Map<Long, PlaceDateTraitsView> findTraitsByPlace(Map<Long, List<Place>> placesByContent) {
        List<Long> placeIds = placeIds(placesByContent);
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return placeClassificationRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(
                        PlaceClassification::getPlaceId,
                        PlaceDateTraitsView::from
                ));
    }

    private Map<Long, Long> countSaves(Map<Long, List<Place>> placesByContent) {
        List<Long> placeIds = placeIds(placesByContent);
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return memberPlaceRepository.countSavesByPlaceIdIn(placeIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private Set<Long> findSavedPlaceIds(
            Long memberId,
            Map<Long, List<Place>> placesByContent
    ) {
        List<Long> placeIds = placeIds(placesByContent);
        if (placeIds.isEmpty()) {
            return Set.of();
        }
        return memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(memberId, placeIds)
                .stream()
                .map(MemberPlace::getPlace)
                .map(Place::getId)
                .collect(Collectors.toSet());
    }

    private List<Long> placeIds(Map<Long, List<Place>> placesByContent) {
        return placesByContent.values().stream()
                .flatMap(List::stream)
                .map(Place::getId)
                .distinct()
                .toList();
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
