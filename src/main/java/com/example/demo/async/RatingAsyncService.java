package com.example.demo.async;

import com.example.demo.dto.ModRating;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class RatingAsyncService {

    private final long delayMillis;

    public RatingAsyncService(@Value("${app.async.rating-delay-ms:1000}") long delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Async(AsyncConfig.EXECUTOR)
    public CompletableFuture<ModRating> fetchRating(Long modId) {
        Delays.pause(delayMillis);
        double average = 3.0 + (modId % 20) / 10.0;
        int votes = (int) (modId * 7 % 100) + 1;
        return CompletableFuture.completedFuture(new ModRating(average, votes));
    }
}
