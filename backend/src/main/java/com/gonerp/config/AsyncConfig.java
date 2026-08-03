package com.gonerp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor cho các job export chạy nền (ZIP report tháng). Giữ pool nhỏ vì mỗi
     * job dựng cả archive trong bộ nhớ — chạy song song nhiều job dễ làm cạn heap.
     */
    @Bean("financeExportExecutor")
    public Executor financeExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("finance-export-");
        executor.initialize();
        return executor;
    }
}
