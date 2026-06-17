package com.personal.todo.modules.task.infra.adapters.libs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.personal.todo.TodoApplication;
import com.personal.todo.modules.task.business.app.ports.output.remotetask.RemoteTasksResponse;

@SpringBootTest(classes = TodoApplication.class)
@ActiveProfiles("test")
public class FetchRemoteTasksTest {
    @MockitoBean
    private WebClient dummyJsonWebClient;
    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private CacheService cacheService;
    @MockitoBean
    private Cache cache;
    @InjectMocks
    private DummyJsonWebClientSyncTaskFetcher fetcher;
    private final Throwable throwable = new RuntimeException("circuit breaker open");

    @Test
    void fetchTasksByUserIdFallbackShouldReturnCachedTasks() {
        Integer userId = 1;
        RemoteTasksResponse expected = new RemoteTasksResponse(List.of(), 10, 1, 20, userId);

        when(cacheManager.getCache("remoteTasks")).thenReturn(cache);
        when(cache.get(userId, RemoteTasksResponse.class)).thenReturn(expected);

        RemoteTasksResponse result = fetcher.fetchTasksByUserIdFallback(userId, throwable);

        assertNotNull(result);
        assertEquals(expected, result);
        assertEquals(userId, result.userId());

        verify(cacheManager).getCache("remoteTasks");
        verify(cache).get(userId, RemoteTasksResponse.class);
    }

    @Test
    void fetchTasksByUserIdFallbackShouldReturnEmptyListOfTasksIfCacheIsMissing() {
        int userId = 1;

        when(cacheManager.getCache("remoteTasks")).thenReturn(cache);
        when(cache.get(userId, RemoteTasksResponse.class)).thenReturn(null);

        RemoteTasksResponse result = fetcher.fetchTasksByUserIdFallback(userId, throwable);
        var expected = new RemoteTasksResponse(List.of(), 0, 0, 0, userId);
        
        assertNotNull(result);
        assertEquals(expected, result);

        verify(cache).get(userId, RemoteTasksResponse.class);
        verify(cacheManager).getCache("remoteTasks");
    }

    @Test
    void fetchTasksByUserIdFallbackShouldReturnEmptyListOfTasksIfCacheIsNull() {
         int userId = 1;

        when(cacheManager.getCache("remoteTasks")).thenReturn(null);

        RemoteTasksResponse result = fetcher.fetchTasksByUserIdFallback(userId, throwable);
        var expected = new RemoteTasksResponse(List.of(), 0, 0, 0, userId);
        
        assertNotNull(result);
        assertEquals(expected, result);

        verify(cacheManager).getCache("remoteTasks");
        verify(cache, Mockito.never()).get(userId, RemoteTasksResponse.class);
    }
}
