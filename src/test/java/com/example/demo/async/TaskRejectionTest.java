package com.example.demo.async;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(TaskRejectionTest.Config.class)
class TaskRejectionTest {

    @Configuration
    @EnableAsync
    static class Config {

        @Bean(name = AsyncConfig.EXECUTOR)
        ThreadPoolTaskExecutor tinyExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            executor.setThreadNamePrefix("tiny-async-");
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
            return executor;
        }

        @Bean
        BlockingTasks blockingTasks() {
            return new BlockingTasks();
        }
    }

    public static class BlockingTasks {

        private final CountDownLatch gate = new CountDownLatch(1);

        @Async(AsyncConfig.EXECUTOR)
        public CompletableFuture<String> run() throws InterruptedException {
            gate.await(5, TimeUnit.SECONDS);
            return CompletableFuture.completedFuture("done");
        }

        public void open() {
            gate.countDown();
        }
    }

    @Autowired
    private BlockingTasks tasks;

    @Test
    void overflowingTinyExecutor_rejectsExtraTaskWithTaskRejectedException() throws Exception {
        CompletableFuture<String> running = tasks.run();
        CompletableFuture<String> queued = tasks.run();

        assertThrows(TaskRejectedException.class, () -> tasks.run());

        tasks.open();
        assertEquals("done", running.get(3, TimeUnit.SECONDS));
        assertEquals("done", queued.get(3, TimeUnit.SECONDS));
    }
}
