package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.PlaceImageBackfillProperties;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "place.image-backfill",
        name = "enabled",
        havingValue = "true"
)
public class PlaceImageBackfillRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImageBackfillRunner.class);

    private final PlaceRepository placeRepository;
    private final PlaceImageEnrichmentService enrichmentService;
    private final PlaceImageBackfillProperties properties;
    private final ConfigurableApplicationContext applicationContext;

    public PlaceImageBackfillRunner(
            PlaceRepository placeRepository,
            PlaceImageEnrichmentService enrichmentService,
            PlaceImageBackfillProperties properties,
            ConfigurableApplicationContext applicationContext
    ) {
        this.placeRepository = placeRepository;
        this.enrichmentService = enrichmentService;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        var places = placeRepository.findAll().stream()
                .filter(place -> !isStored(place.getThumbnailUrl()))
                .limit(properties.maxPlaces())
                .toList();
        for (int index = 0; index < places.size(); index++) {
            enrichmentService.enrichPlace(places.get(index).getId());
            waitBetweenRequests(index, places.size());
        }
        logger.info("Kakao place image backfill completed: total={}", places.size());
        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }

    private boolean isStored(String imageUrl) {
        return imageUrl != null && imageUrl.contains("/api/v1/place-images/");
    }

    private void waitBetweenRequests(int index, int total) {
        if (index == total - 1 || properties.delayMillis() == 0) {
            return;
        }
        try {
            Thread.sleep(properties.delayMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Place image backfill interrupted", exception);
        }
    }
}
