package kr.omong.dulpick.domain.place.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class PlaceImageWorkerConfig {

    private static final int WORKER_CONCURRENCY = 2;
    private static final int QUEUE_CAPACITY = 100;

    @Bean(name = "placeImageExecutor")
    public Executor placeImageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(WORKER_CONCURRENCY);
        executor.setMaxPoolSize(WORKER_CONCURRENCY);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("place-image-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(1);
        executor.initialize();
        return executor;
    }
}
