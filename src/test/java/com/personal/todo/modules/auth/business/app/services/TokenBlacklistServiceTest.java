package com.personal.todo.modules.auth.business.app.services;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {
    @Mock
    private HazelcastInstance hazelcastInstance;
    @Mock
    @SuppressWarnings("rawtypes")
    private IMap tokenMap;
    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        reset(hazelcastInstance, tokenMap);
    }

    @Test
    void shouldBlacklistTokenSuccessfully() {
        // Arrange
        String testToken = "test.jwt.token.123";
        when(hazelcastInstance.getMap(anyString())).thenReturn(tokenMap);
        
        // Act
        tokenBlacklistService.blacklist(testToken);

        // Assert
        verify(tokenMap).put(testToken, Boolean.TRUE, 2L, TimeUnit.HOURS);
    }

    @Test
    void shouldNotBlacklistNullToken() {
        // Act
        tokenBlacklistService.blacklist(null);

        // Assert
        verify(tokenMap, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void shouldNotBlacklistBlankToken() {
        // Act
        tokenBlacklistService.blacklist("");

        // Assert
        verify(tokenMap, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void shouldNotBlacklistBlankSpaceToken() {
        // Act
        tokenBlacklistService.blacklist("   ");

        // Assert
        verify(tokenMap, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void shouldIsBlacklistedReturnTrueForBlacklistedToken() {
        // Arrange
        String testToken = "test.jwt.token.123";
        when(hazelcastInstance.getMap(anyString())).thenReturn(tokenMap);
        when(tokenMap.containsKey(testToken)).thenReturn(true);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Assert
        assertTrue(result);
        verify(tokenMap).containsKey(testToken);
    }

    @Test
    void shouldIsBlacklistedReturnFalseForNonBlacklistedToken() {
        // Arrange
        String testToken = "test.jwt.token.123";
        when(hazelcastInstance.getMap(anyString())).thenReturn(tokenMap);
        when(tokenMap.containsKey(testToken)).thenReturn(false);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Assert
        assertFalse(result);
        verify(tokenMap).containsKey(testToken);
    }

    @Test
    void shouldIsBlacklistedReturnFalseForNullToken() {
        // Act
        boolean result = tokenBlacklistService.isBlacklisted(null);

        // Assert
        assertFalse(result);
        verify(tokenMap, never()).containsKey(any());
    }

    @Test
    void shouldUseCorrectMapName() {
        // Arrange
        String expectedMapName = "token-blacklist";
        when(hazelcastInstance.getMap(expectedMapName)).thenReturn(tokenMap);
        
        // Act
        tokenBlacklistService.blacklist("test.token");
        
        // Assert
        verify(hazelcastInstance).getMap(expectedMapName);
    }

    @Test
    void shouldUseCorrectTTL() {
        // Arrange
        when(hazelcastInstance.getMap(anyString())).thenReturn(tokenMap);
        
        // Act
        tokenBlacklistService.blacklist("test.token");
        
        // Assert
        verify(tokenMap).put(anyString(), eq(Boolean.TRUE), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    void shouldIsBlacklistedUseCorrectMap() {
        // Arrange
        String testToken = "test.jwt.token.123";
        when(hazelcastInstance.getMap(anyString())).thenReturn(tokenMap);
        when(tokenMap.containsKey(testToken)).thenReturn(true);
        
        // Act
        tokenBlacklistService.isBlacklisted(testToken);
        
        // Assert
        verify(hazelcastInstance).getMap("token-blacklist");
        verify(tokenMap).containsKey(testToken);
    }
}
