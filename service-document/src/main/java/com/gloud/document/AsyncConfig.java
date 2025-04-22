package com.gloud.document;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);              // 🟢 항상 유지되는 최소 스레드 수 t2.small : 2  / t2.medium : 4 추천
        executor.setMaxPoolSize(10);              // 🔵 최대 동시에 생성될 수 있는 스레드 수  4 / 8
        executor.setQueueCapacity(100);           // 🟡 대기 큐에 쌓을 수 있는 작업 수     50 / 100
        executor.setThreadNamePrefix("Async-");   // 🔤 로그 구분용 스레드 접두사
        executor.initialize();                    // ⚙️ 스레드풀 초기화
        return executor;
    }
}