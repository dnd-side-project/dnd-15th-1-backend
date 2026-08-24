package kr.omong.dulpick.domain.place.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ContentImageWorkerConfig {

    @Bean(name = "contentImageExecutor")
    public Executor contentImageExecutor(ContentThumbnailProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.workerConcurrency());
        executor.setMaxPoolSize(properties.workerConcurrency());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("content-image-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
