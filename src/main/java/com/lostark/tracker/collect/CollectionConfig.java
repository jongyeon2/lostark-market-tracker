package com.lostark.tracker.collect;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables the collection tick scheduler ({@link EnableScheduling}) and the async fan-out
 * ({@link EnableAsync}), and provides the dedicated bounded executor the per-item fetches run on
 * (D-06). A dedicated pool keeps collection work off the common ForkJoinPool and bounds
 * concurrency well under the rate limiter / 100-per-min budget.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class CollectionConfig {

    public static final String COLLECTION_EXECUTOR = "collectionTaskExecutor";

    @Bean(COLLECTION_EXECUTOR)
    public ThreadPoolTaskExecutor collectionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Sized for a ~10-20 item fan-out; constants (externalization to properties is v2).
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("collect-");
        executor.initialize();
        return executor;
    }
}
