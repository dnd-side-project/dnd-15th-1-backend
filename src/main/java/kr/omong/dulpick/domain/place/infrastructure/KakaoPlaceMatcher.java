package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class KakaoPlaceMatcher {

    private static final Pattern NUMBERED_ADDRESS = Pattern.compile(
            "([\\p{L}\\p{N}]+(?:로|길|동|가|읍|면|리))\\s*(\\d+(?:-\\d+)?)"
    );
    private static final Set<String> SOFT_HINT_STOP_WORDS = Set.of(
            "거리", "근처", "주변", "인근", "도보", "위치", "주소", "에서"
    );
    private static final Map<String, String> PROVINCE_ALIASES = provinceAliases();

    Optional<MatchedPlace> findBest(
            ExtractedPlace extractedPlace,
            List<PlaceSearchResult> results
    ) {
        return results.stream()
                .map(result -> match(extractedPlace, result))
                .flatMap(Optional::stream)
                .max(Comparator.comparingInt(MatchedPlace::score));
    }

    boolean hasPreciseAddress(String addressHint) {
        return !addressKeys(addressHint).isEmpty();
    }

    boolean hasDefinitiveMatch(
            ExtractedPlace extractedPlace,
            List<PlaceSearchResult> results
    ) {
        return results.stream().anyMatch(result -> isDefinitiveMatch(extractedPlace, result));
    }

    private boolean isDefinitiveMatch(
            ExtractedPlace extractedPlace,
            PlaceSearchResult result
    ) {
        String addressHint = extractedPlace.addressHint();
        String resultAddress = resultAddress(result);
        if (hasAdministrativeConflict(addressHint, resultAddress)) {
            return false;
        }
        if (nameQuality(extractedPlace.name(), result.name()) != NameQuality.EXACT) {
            return false;
        }
        if (!hasPreciseAddress(addressHint)) {
            return true;
        }
        return addressQuality(addressHint, resultAddress) == AddressQuality.EXACT;
    }

    private Optional<MatchedPlace> match(
            ExtractedPlace extractedPlace,
            PlaceSearchResult result
    ) {
        String addressHint = extractedPlace.addressHint();
        String resultAddress = resultAddress(result);
        if (hasAdministrativeConflict(addressHint, resultAddress)) {
            return Optional.empty();
        }
        NameQuality nameQuality = nameQuality(extractedPlace.name(), result.name());
        AddressQuality addressQuality = addressQuality(addressHint, resultAddress);
        if (addressQuality == AddressQuality.CONFLICT) {
            return Optional.empty();
        }
        if (nameQuality == NameQuality.NONE && !addressQuality.supportsAliasReview()) {
            return Optional.empty();
        }
        PlaceVerificationStatus status = status(nameQuality, addressQuality);
        int score = nameQuality.score + addressQuality.score + evidenceScore(extractedPlace);
        return Optional.of(new MatchedPlace(result, status, score));
    }

    private PlaceVerificationStatus status(
            NameQuality nameQuality,
            AddressQuality addressQuality
    ) {
        if (nameQuality != NameQuality.NONE && !addressQuality.requiresReview()) {
            return PlaceVerificationStatus.VERIFIED;
        }
        return PlaceVerificationStatus.REVIEW_REQUIRED;
    }

    private NameQuality nameQuality(String extractedName, String resultName) {
        String extracted = normalize(extractedName);
        String result = normalize(resultName);
        if (extracted.isBlank() || result.isBlank()) {
            return NameQuality.NONE;
        }
        if (result.equals(extracted)) {
            return NameQuality.EXACT;
        }
        if (Math.min(result.length(), extracted.length()) >= 2
                && (result.contains(extracted) || extracted.contains(result))) {
            return NameQuality.PARTIAL;
        }
        return NameQuality.NONE;
    }

    private AddressQuality addressQuality(String addressHint, String resultAddress) {
        List<AddressKey> expectedKeys = addressKeys(addressHint);
        if (!expectedKeys.isEmpty()) {
            List<AddressKey> resultKeys = addressKeys(resultAddress);
            if (hasExactAddress(expectedKeys, resultKeys)) {
                return AddressQuality.EXACT;
            }
            if (hasSameRoad(expectedKeys, resultKeys)) {
                return AddressQuality.SAME_ROAD;
            }
            return AddressQuality.CONFLICT;
        }
        if (addressHint == null || addressHint.isBlank()) {
            return AddressQuality.NONE;
        }
        return hasSoftLocationMatch(addressHint, resultAddress)
                ? AddressQuality.LOCATION
                : AddressQuality.NONE;
    }

    private boolean hasExactAddress(List<AddressKey> expected, List<AddressKey> results) {
        return expected.stream().anyMatch(results::contains);
    }

    private boolean hasSameRoad(List<AddressKey> expected, List<AddressKey> results) {
        Set<String> resultRoads = new HashSet<>();
        results.forEach(result -> resultRoads.add(result.road()));
        return expected.stream().map(AddressKey::road).anyMatch(resultRoads::contains);
    }

    private List<AddressKey> addressKeys(String address) {
        if (address == null || address.isBlank()) {
            return List.of();
        }
        Matcher matcher = NUMBERED_ADDRESS.matcher(address);
        List<AddressKey> keys = new ArrayList<>();
        while (matcher.find()) {
            keys.add(new AddressKey(normalize(matcher.group(1)), matcher.group(2)));
        }
        return keys;
    }

    private boolean hasSoftLocationMatch(String addressHint, String resultAddress) {
        String normalizedResult = normalize(resultAddress);
        return softLocationTokens(addressHint).stream().anyMatch(normalizedResult::contains);
    }

    private List<String> softLocationTokens(String addressHint) {
        if (addressHint == null || addressHint.isBlank()) {
            return List.of();
        }
        return List.of(addressHint.strip().split("\\s+"))
                .stream()
                .map(this::stripSoftSuffix)
                .map(this::normalize)
                .filter(token -> token.length() >= 2)
                .filter(token -> token.chars().noneMatch(Character::isDigit))
                .filter(token -> !SOFT_HINT_STOP_WORDS.contains(token))
                .toList();
    }

    private String stripSoftSuffix(String token) {
        return token.replaceAll("(?:에서|으로|까지|부터|근처|주변|인근|역)$", "");
    }

    private boolean hasAdministrativeConflict(String first, String second) {
        String firstProvince = province(first);
        String secondProvince = province(second);
        if (firstProvince != null && secondProvince != null
                && !firstProvince.equals(secondProvince)) {
            return true;
        }
        return hasRegionLevelConflict(first, second, "시")
                || hasRegionLevelConflict(first, second, "군")
                || hasRegionLevelConflict(first, second, "구");
    }

    private boolean hasRegionLevelConflict(String first, String second, String suffix) {
        Set<String> firstRegions = regionTokens(first, suffix);
        Set<String> secondRegions = regionTokens(second, suffix);
        if (firstRegions.isEmpty() || secondRegions.isEmpty()) {
            return false;
        }
        Set<String> intersection = new HashSet<>(firstRegions);
        intersection.retainAll(secondRegions);
        return intersection.isEmpty();
    }

    private Set<String> regionTokens(String address, String suffix) {
        if (address == null || address.isBlank()) {
            return Set.of();
        }
        Set<String> regions = new HashSet<>();
        for (String token : address.strip().split("\\s+")) {
            String cleaned = token.replaceAll("[^\\p{L}\\p{N}]", "");
            if (cleaned.endsWith(suffix) && !PROVINCE_ALIASES.containsKey(cleaned)) {
                regions.add(cleaned);
            }
        }
        return regions;
    }

    private String province(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        for (String token : address.strip().split("\\s+")) {
            String province = PROVINCE_ALIASES.get(token);
            if (province != null) {
                return province;
            }
        }
        return null;
    }

    private int evidenceScore(ExtractedPlace extractedPlace) {
        String evidence = normalize(extractedPlace.evidence());
        return evidence.contains(normalize(extractedPlace.name())) ? 10 : 0;
    }

    private String resultAddress(PlaceSearchResult result) {
        return (safe(result.address()) + " " + safe(result.roadAddress())).strip();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static Map<String, String> provinceAliases() {
        Map<String, String> aliases = new HashMap<>();
        registerProvince(aliases, "서울", "서울", "서울시", "서울특별시");
        registerProvince(aliases, "부산", "부산", "부산시", "부산광역시");
        registerProvince(aliases, "대구", "대구", "대구시", "대구광역시");
        registerProvince(aliases, "인천", "인천", "인천시", "인천광역시");
        registerProvince(aliases, "광주", "광주", "광주시", "광주광역시");
        registerProvince(aliases, "대전", "대전", "대전시", "대전광역시");
        registerProvince(aliases, "울산", "울산", "울산시", "울산광역시");
        registerProvince(aliases, "세종", "세종", "세종시", "세종특별자치시");
        registerProvince(aliases, "경기", "경기", "경기도");
        registerProvince(aliases, "강원", "강원", "강원도", "강원특별자치도");
        registerProvince(aliases, "충북", "충북", "충청북도");
        registerProvince(aliases, "충남", "충남", "충청남도");
        registerProvince(aliases, "전북", "전북", "전라북도", "전북특별자치도");
        registerProvince(aliases, "전남", "전남", "전라남도");
        registerProvince(aliases, "경북", "경북", "경상북도");
        registerProvince(aliases, "경남", "경남", "경상남도");
        registerProvince(aliases, "제주", "제주", "제주도", "제주특별자치도");
        return Map.copyOf(aliases);
    }

    private static void registerProvince(
            Map<String, String> aliases,
            String canonical,
            String... names
    ) {
        for (String name : names) {
            aliases.put(name, canonical);
        }
    }

    record MatchedPlace(
            PlaceSearchResult place,
            PlaceVerificationStatus status,
            int score
    ) {
    }

    private record AddressKey(String road, String number) {
    }

    private enum NameQuality {
        NONE(0),
        PARTIAL(70),
        EXACT(100);

        private final int score;

        NameQuality(int score) {
            this.score = score;
        }
    }

    private enum AddressQuality {
        CONFLICT(-1),
        NONE(0),
        LOCATION(30),
        SAME_ROAD(55),
        EXACT(100);

        private final int score;

        AddressQuality(int score) {
            this.score = score;
        }

        private boolean supportsAliasReview() {
            return this == LOCATION || this == SAME_ROAD || this == EXACT;
        }

        private boolean requiresReview() {
            return this == SAME_ROAD;
        }
    }
}
