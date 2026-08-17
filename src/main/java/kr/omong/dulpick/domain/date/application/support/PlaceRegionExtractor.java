package kr.omong.dulpick.domain.date.application.support;

import org.springframework.stereotype.Component;

@Component
public class PlaceRegionExtractor {

    public String extract(String roadAddress, String address) {
        String source = chooseAddress(roadAddress, address);
        if (source.isBlank()) {
            return "기타";
        }
        String[] tokens = source.split("\\s+");
        if (tokens.length >= 2 && isRegionToken(tokens[1])) {
            return tokens[1];
        }
        if (tokens.length >= 3 && isRegionToken(tokens[2])) {
            return tokens[2];
        }
        return tokens[0];
    }

    public boolean matchesRegionFilter(
            String region,
            String regionFilter
    ) {
        if (regionFilter == null || regionFilter.isBlank()) {
            return true;
        }
        return normalize(region).contains(normalize(regionFilter));
    }

    private String chooseAddress(String roadAddress, String address) {
        if (roadAddress != null && !roadAddress.isBlank()) {
            return roadAddress.strip();
        }
        return address == null ? "" : address.strip();
    }

    private boolean isRegionToken(String token) {
        return token.endsWith("시")
                || token.endsWith("군")
                || token.endsWith("구");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "");
    }
}
