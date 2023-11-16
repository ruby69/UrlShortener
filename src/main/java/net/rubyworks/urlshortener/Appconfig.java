package net.rubyworks.urlshortener;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class Appconfig {
    private int availableProcessors = Runtime.getRuntime().availableProcessors();

    @Bean
    ThreadPoolTaskExecutor fluxPool() {
        int size = availableProcessors * 2;
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(size);
        taskExecutor.setMaxPoolSize(size);
        taskExecutor.setQueueCapacity(1000000);
        taskExecutor.setThreadNamePrefix("fluxPool-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        return taskExecutor;
    }
}
