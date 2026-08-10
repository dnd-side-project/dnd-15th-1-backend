package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class KakaoPlaceMatcher {

    Optional<PlaceSearchResult> findBest(
            ExtractedPlace extractedPlace,
            List<PlaceSearchResult> results
    ) {
        return results.stream()
                .filter(result -> matchesName(extractedPlace.name(), result.name()))
                .filter(result -> matchesAddress(extractedPlace.addressHint(), result))
                .max(Comparator.comparingInt(result -> score(extractedPlace, result)));
    }

    private boolean matchesName(String extractedName, String resultName) {
        String extracted = normalize(extractedName);
        String result = normalize(resultName);
        return !extracted.isBlank()
                && (result.equals(extracted)
                || result.contains(extracted)
                || extracted.contains(result));
    }

    private boolean matchesAddress(String addressHint, PlaceSearchResult result) {
        if (addressHint == null || addressHint.isBlank()) {
            return true;
        }
        return addressMatchScore(addressHint, result) > 0;
    }

    private int score(ExtractedPlace extractedPlace, PlaceSearchResult result) {
        int score = normalize(result.name()).equals(normalize(extractedPlace.name())) ? 25 : 15;
        score += evidenceScore(extractedPlace);
        score += addressMatchScore(extractedPlace.addressHint(), result);
        if ("EXPLICIT_VENUE".equalsIgnoreCase(extractedPlace.mentionType())) {
            score += 10;
        }
        return score;
    }

    private int evidenceScore(ExtractedPlace extractedPlace) {
        String rawEvidence = extractedPlace.evidence() == null
                ? ""
                : extractedPlace.evidence().strip();
        if (rawEvidence.isBlank()) {
            return -40;
        }
        int score = normalize(rawEvidence).contains(normalize(extractedPlace.name())) ? 30 : 0;
        if (rawEvidence.contains("위치") || rawEvidence.contains("주소") || rawEvidence.contains("📍")) {
            score += 20;
        }
        return score;
    }

    private int addressMatchScore(String addressHint, PlaceSearchResult result) {
        if (addressHint == null || addressHint.isBlank()) {
            return 0;
        }
        String resultAddress = result.address() + " " + result.roadAddress();
        if (containsEither(normalize(addressHint), normalize(resultAddress))) {
            return 40;
        }
        return sharesLocationToken(addressHint, resultAddress) ? 30 : -30;
    }

    private boolean containsEither(String first, String second) {
        return !first.isBlank()
                && !second.isBlank()
                && (first.contains(second) || second.contains(first));
    }

    private boolean sharesLocationToken(String first, String second) {
        Set<String> tokens = new HashSet<>(locationTokens(first));
        tokens.retainAll(locationTokens(second));
        return !tokens.isEmpty();
    }

    private List<String> locationTokens(String address) {
        return Arrays.stream(address.strip().split("\\s+"))
                .map(token -> token.replaceAll("[^\\p{L}\\p{N}]", ""))
                .filter(token -> token.length() >= 2)
                .filter(token -> token.matches(".*(?:로|길|동|가|읍|면|리)$"))
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
