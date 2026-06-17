package com.personal.todo.modules.task.infra.adapters.libs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.personal.todo.TodoApplication;
import com.personal.todo.modules.task.business.app.ports.output.remotetask.RemoteTask;
import com.personal.todo.modules.task.business.app.ports.output.remotetask.RemoteTasksResponse;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest(classes = TodoApplication.class)
@ActiveProfiles("test")
public class FetchRemoteTasksCircuitBreakerTest {
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    @Qualifier("dummyJsonWebClientSyncTaskFetcher")
    private DummyJsonWebClientSyncTaskFetcher fetcher;
    @MockitoBean
    private WebClient dummyJsonWebClient;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry
            .find("fetchTasksByUserId")
            .ifPresent(CircuitBreaker::reset);
    }

    @Test
    void shouldOpenCircuitAfterFailureThreshold() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(dummyJsonWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RemoteTasksResponse.class))
            .thenThrow(new RuntimeException("service unavailable"));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchTasksByUserId");

        int userId = 1;
        var expectedRmoteTasks =  new RemoteTasksResponse(List.of(), 0, 0, 0, userId);
        
        for (int i = 0; i < 4; i++) {
            var result = fetcher.fetchTasksByUserId(userId);
            assertEquals(expectedRmoteTasks, result);
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // garantir que metodo cache sucess nao foi chamado
        // o cache deve estar vazio
    }

    @Test
    void circuitBreakerShouldContinueClosedIfThresholdHitedWihtNoFailure() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        var remoteTasks = List.of(
            new RemoteTask(1, "task 1", true),
            new RemoteTask(2, "task 2", false)
        );

        int userId = 1;
        
        var remoteTasksResponse = new RemoteTasksResponse(
            remoteTasks,
            2,
            0,
            0,
            userId
        ); 
        
        when(dummyJsonWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RemoteTasksResponse.class))
        .thenReturn(Mono.just(remoteTasksResponse));
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchTasksByUserId");
        
        Cache cache = cacheManager.getCache("remoteTasks");

        for (int i = 0; i < 4; i++) {
            fetcher.fetchTasksByUserId(userId);

            var cachedRemoteTasks = cache.get(userId, RemoteTasksResponse.class);
            assertEquals(remoteTasksResponse.todos(), cachedRemoteTasks.todos());
            assertEquals(2, cachedRemoteTasks.total());
        }
        
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        // garantir se o metodo cache success foi chamando
    }

    @Test
    void shouldReturnCachedTasksIfCircuitBreakerIsOpen() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(dummyJsonWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RemoteTasksResponse.class))
            .thenThrow(new RuntimeException("service unavailable"));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchTasksByUserId");

        var remoteTasks = List.of(
            new RemoteTask(1, "task 1", true),
            new RemoteTask(2, "task 2", false)
        );
        
        int userId = 1;
        var cachedRemoteTasksResponse = new RemoteTasksResponse(
            remoteTasks,
            2,
            0,
            0,
            userId
        ); 
        
        Cache cache = cacheManager.getCache("remoteTasks");
        cache.put(userId, cachedRemoteTasksResponse);

        RemoteTasksResponse result = null;

        for (int i = 0; i < 4; i++) {
            result = fetcher.fetchTasksByUserId(userId);
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(cachedRemoteTasksResponse, result);
    }

    @Test
    void shouldTransitionToHalfOpenAndCloseOnSuccess() throws InterruptedException {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(dummyJsonWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RemoteTasksResponse.class))
            .thenThrow(new RuntimeException("service unavailable"));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchTasksByUserId");
        int userId = 1;

        // Trigger failures to open the circuit
        for (int i = 0; i < 4; i++) {
            fetcher.fetchTasksByUserId(userId);
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Wait for the wait duration to pass so circuit transitions to HALF_OPEN
        Thread.sleep(1500);

        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());

        var remoteTasks = List.of(
            new RemoteTask(1, "task 1", true),
            new RemoteTask(2, "task 2", false)
        );
        var remoteTasksResponse = new RemoteTasksResponse(remoteTasks, 2, 0, 0, userId);

        // Reset the mock to clear the thenThrow() stubbing and set up success response
        reset(responseSpec);
        when(responseSpec.bodyToMono(RemoteTasksResponse.class))
            .thenReturn(Mono.just(remoteTasksResponse));

        // Make a successful call in HALF_OPEN state
        for (int i = 0; i < 2; i++) {
            RemoteTasksResponse result = fetcher.fetchTasksByUserId(userId);
            assertEquals(remoteTasksResponse, result);
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void shouldReopenCircuitOnFailureInHalfOpenState() throws InterruptedException {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(dummyJsonWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RemoteTasksResponse.class))
            .thenThrow(new RuntimeException("service unavailable"));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchTasksByUserId");
        int userId = 1;

        // Trigger failures to open the circuit
        for (int i = 0; i < 4; i++) {
            fetcher.fetchTasksByUserId(userId);
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Wait for the wait duration to pass so circuit transitions to HALF_OPEN
        Thread.sleep(1500);

        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());

        // Reset the mock to ensure it still throws (failure in HALF_OPEN state)
        reset(responseSpec);
        when(responseSpec.bodyToMono(RemoteTasksResponse.class))
            .thenThrow(new RuntimeException("service unavailable"));

        // Make a call that will fail in HALF_OPEN state
        for (int i = 0; i < 2; i++) {
            RemoteTasksResponse result = fetcher.fetchTasksByUserId(userId);
            assertEquals(new RemoteTasksResponse(List.of(), 0, 0, 0, userId), result);
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }
}
