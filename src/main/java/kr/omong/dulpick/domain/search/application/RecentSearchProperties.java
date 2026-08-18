package kr.omong.dulpick.domain.search.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("search.recent")
public record RecentSearchProperties(int maxPerType) {

    public RecentSearchProperties {
        if (maxPerType < 1 || maxPerType > 200) {
            throw new IllegalArgumentException("search.recent.max-per-type must be between 1 and 200");
        }
    }
}
