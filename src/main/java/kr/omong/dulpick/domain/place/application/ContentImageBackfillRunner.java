package kr.omong.dulpick.domain.place.application;

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
        prefix = "content.image-backfill",
        name = "enabled",
        havingValue = "true"
)
public class ContentImageBackfillRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ContentImageBackfillRunner.class);

    private final ContentImageBackfillService backfillService;
    private final ConfigurableApplicationContext applicationContext;

    public ContentImageBackfillRunner(
            ContentImageBackfillService backfillService,
            ConfigurableApplicationContext applicationContext
    ) {
        this.backfillService = backfillService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        ContentImageBackfillService.Result result = backfillService.backfill();
        logger.info(
                "Instagram image backfill completed: total={}, succeeded={}, failed={}",
                result.total(),
                result.succeeded(),
                result.failed()
        );
        int exitCode = SpringApplication.exit(applicationContext, () -> result.failed() == 0 ? 0 : 1);
        System.exit(exitCode);
    }
}
