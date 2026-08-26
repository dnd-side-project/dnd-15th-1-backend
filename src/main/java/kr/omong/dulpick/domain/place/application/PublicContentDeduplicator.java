package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class PublicContentDeduplicator {

    private PublicContentDeduplicator() {
    }

    static Result deduplicate(Collection<Content> contents) {
        Map<String, List<Content>> groups = contents.stream()
                .collect(Collectors.groupingBy(
                        PublicContentDeduplicator::identityOf,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, List<Long>> sourceIdsByRepresentative = new LinkedHashMap<>();
        List<Content> representatives = groups.values().stream()
                .map(group -> representative(group, sourceIdsByRepresentative))
                .toList();
        return new Result(representatives, sourceIdsByRepresentative);
    }

    private static Content representative(
            List<Content> group,
            Map<Long, List<Long>> sourceIdsByRepresentative
    ) {
        Content representative = group.stream()
                .reduce(PublicContentDeduplicator::prefer)
                .orElseThrow();
        sourceIdsByRepresentative.put(
                representative.getId(),
                group.stream().map(Content::getId).toList()
        );
        return representative;
    }

    private static Content prefer(Content first, Content second) {
        return completeness(first).compareTo(completeness(second)) >= 0 ? first : second;
    }

    private static ComparatorKey completeness(Content content) {
        return new ComparatorKey(
                content.getPlaceCount(),
                present(content.getThumbnailUrl()),
                present(content.getTitle()),
                present(content.getContent()),
                instantValue(content.getUpdatedAt()),
                instantValue(content.getCreatedAt()),
                content.getId() == null ? 0L : content.getId()
        );
    }

    private static String identityOf(Content content) {
        if (content.getSourceType() != ContentSourceType.INSTAGRAM_REEL
                && content.getSourceType() != ContentSourceType.INSTAGRAM_POST) {
            return "CONTENT:" + content.getId();
        }
        String mediaKey = instagramMediaKey(content.getCanonicalUrl());
        return mediaKey == null ? "CONTENT:" + content.getId() : "INSTAGRAM:" + mediaKey;
    }

    static String instagramMediaKey(String canonicalUrl) {
        try {
            URI uri = new URI(canonicalUrl);
            if (!isInstagramHost(uri.getHost())) {
                return null;
            }
            String path = uri.getPath().replaceAll("/+$", "");
            String[] segments = path.split("/");
            if (segments.length != 3
                    || (!"p".equals(segments[1])
                    && !"posts".equals(segments[1])
                    && !"reel".equals(segments[1]))
                    || segments[2].isBlank()) {
                return null;
            }
            return segments[2];
        } catch (URISyntaxException | NullPointerException exception) {
            return null;
        }
    }

    private static boolean isInstagramHost(String host) {
        return "instagram.com".equalsIgnoreCase(host)
                || "www.instagram.com".equalsIgnoreCase(host);
    }

    private static int present(String value) {
        return value == null || value.isBlank() ? 0 : 1;
    }

    private static Instant instantValue(Instant value) {
        return value == null ? Instant.MIN : value;
    }

    private record ComparatorKey(
            int placeCount,
            int thumbnailPresent,
            int titlePresent,
            int contentPresent,
            Instant updatedAt,
            Instant createdAt,
            long id
    ) implements Comparable<ComparatorKey> {

        @Override
        public int compareTo(ComparatorKey other) {
            int result = Integer.compare(placeCount, other.placeCount);
            if (result != 0) {
                return result;
            }
            result = Integer.compare(thumbnailPresent, other.thumbnailPresent);
            if (result != 0) {
                return result;
            }
            result = Integer.compare(titlePresent, other.titlePresent);
            if (result != 0) {
                return result;
            }
            result = Integer.compare(contentPresent, other.contentPresent);
            if (result != 0) {
                return result;
            }
            result = updatedAt.compareTo(other.updatedAt);
            if (result != 0) {
                return result;
            }
            result = createdAt.compareTo(other.createdAt);
            return result != 0 ? result : Long.compare(id, other.id);
        }
    }

    record Result(
            List<Content> contents,
            Map<Long, List<Long>> sourceIdsByRepresentative
    ) {
    }
}
