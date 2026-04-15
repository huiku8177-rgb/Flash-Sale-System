package com.flashsale.authservice.service;

import java.time.Duration;

public interface AuthSessionService {

    long getCurrentTokenVersion(Long userId);

    void blacklistToken(Long userId, String authorization, Duration fallbackTtl);

    long incrementTokenVersion(Long userId);
}
