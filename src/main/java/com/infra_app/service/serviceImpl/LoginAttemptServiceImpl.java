package com.infra_app.service.serviceImpl;

import com.infra_app.service.LoginAttemptService;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 15 * 60 * 1000;

    private final ConcurrentMap<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> blockedCache = new ConcurrentHashMap<>();

    @Override
    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
        blockedCache.remove(key);
    }

    @Override
    public void loginFailed(String key) {
        int attempts = attemptsCache.getOrDefault(key, 0) + 1;
        attemptsCache.put(key, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            blockedCache.put(key, System.currentTimeMillis() + BLOCK_DURATION_MS);
        }
    }

    @Override
    public boolean isBlocked(String key) {
        Long unblockTime = blockedCache.get(key);
        if (unblockTime == null) {
            return false;
        }

        if (unblockTime < System.currentTimeMillis()) {
            blockedCache.remove(key);
            attemptsCache.remove(key);
            return false;
        }

        return true;
    }
}