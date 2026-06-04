package com.personal.todo.modules.auth.business.app.services;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenBlacklistService {
    private static final String MAP_NAME = "token-blacklist";
    // set 2h TTL for blacklist entries.
    private static final long TOKEN_TTL_HOURS = 2L;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private IMap<String, Boolean> tokenMap() {
        return hazelcastInstance.getMap(MAP_NAME);
    }

    public void blacklist(String token) {
        if (token != null && !token.isBlank()) {
            tokenMap().put(token, Boolean.TRUE, TOKEN_TTL_HOURS, TimeUnit.HOURS);
        }
    }

    public boolean isBlacklisted(String token) {
        return token != null && tokenMap().containsKey(token);
    }
}
