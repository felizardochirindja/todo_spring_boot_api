package com.personal.todo.modules.task.infra.adapters.libs;

import com.personal.todo.modules.task.business.app.ports.output.remotetask.RemoteTaskSyncFetcher;
import com.personal.todo.modules.task.business.app.ports.output.remotetask.RemoteTasksResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;

@Component
public class DummyJsonWebClientSyncTaskFetcher implements RemoteTaskSyncFetcher {
    private static final Logger logger = LoggerFactory.getLogger(DummyJsonWebClientSyncTaskFetcher.class);
    @Autowired
    private WebClient dummyJsonWebClient;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private Cache cache;

    public DummyJsonWebClientSyncTaskFetcher() {
        cache = cacheManager.getCache("remoteTasks");

        if (cache == null) {
            throw new RuntimeException("cache is null");
        }
    }

    @CircuitBreaker(name = "fetchTasksByUserId", fallbackMethod = "fetchTasksByUserIdFallback")
    public RemoteTasksResponse fetchTasksByUserId(Integer userId) {
        var response = dummyJsonWebClient.get()
                .uri("/todos/user/" + userId)
                .retrieve()
                .bodyToMono(RemoteTasksResponse.class)
                .doOnNext(remoteTasks -> {
                    logger.atInfo()
                        .setMessage("Caching successful response for fetchTasksByUserId")
                        .addKeyValue("userId", userId)
                        .log();
                    
                    cache.put(remoteTasks, userId);
                })
                .block();

        return response;
    }

    public RemoteTasksResponse fetchTasksByUserIdFallback(Integer userId, Throwable throwable) {
        logger.atWarn()
            .setMessage("Fallback triggered for fetchTasksByUserId due to circuit breaker exception!")
            .addKeyValue("userId", userId)
            .addKeyValue("exception", throwable.getClass().getSimpleName())
            .addKeyValue("message", throwable.getMessage())
            .log();

        var cachedRemoteTasks = cache.get(userId, RemoteTasksResponse.class);

        if (cachedRemoteTasks == null) {
            logger.atWarn()
                .setMessage("cache miss for fetchTasksByUserId, returning empty response")
                .addKeyValue("userId", userId)
                .log();

            return new RemoteTasksResponse(List.of(), 0, 0, 0, userId);
        }

        logger.atWarn()
            .setMessage("cache hit for fetchTasksByUserId")
            .addKeyValue("userId", userId)
            .addKeyValue("exception", throwable.getClass().getSimpleName())
            .addKeyValue("message", throwable.getMessage())
            .log();

        return cachedRemoteTasks;
    }
}
