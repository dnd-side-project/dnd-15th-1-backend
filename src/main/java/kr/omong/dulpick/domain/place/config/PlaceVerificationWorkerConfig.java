package kr.omong.dulpick.domain.place.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class PlaceVerificationWorkerConfig {

    @Bean(name = "placeVerificationExecutor")
    public Executor placeVerificationExecutor(PlaceAnalysisProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.verificationConcurrency());
        executor.setMaxPoolSize(properties.verificationConcurrency());
        executor.setQueueCapacity(properties.workerConcurrency() * properties.maxCandidates());
        executor.setThreadNamePrefix("place-verification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(1);
        executor.initialize();
        return executor;
    }
}
