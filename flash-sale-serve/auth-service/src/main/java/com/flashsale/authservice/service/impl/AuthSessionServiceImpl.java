package com.flashsale.authservice.service.impl;

import com.flashsale.authservice.service.AuthSessionService;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.CommonException;
import com.flashsale.common.exception.UnauthorizedException;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.common.util.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {

    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTool jwtTool;

    @Override
    public long getCurrentTokenVersion(Long userId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisKeys.authTokenVersion(userId));
            if (!StringUtils.hasText(value)) {
                return 0L;
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            log.error("Invalid token version stored in Redis, userId={}", userId, ex);
            throw serverError("登录态数据异常");
        } catch (Exception ex) {
            log.error("Failed to load token version from Redis, userId={}", userId, ex);
            throw serverError("登录态缓存不可用");
        }
    }

    @Override
    public void blacklistToken(Long userId, String authorization, Duration fallbackTtl) {
        String token = extractBearerToken(authorization);
        JwtTool.TokenClaims claims = jwtTool.parseTokenClaims(token);
        if (!userId.equals(claims.userId())) {
            throw new UnauthorizedException("登录令牌与当前用户不匹配");
        }

        Duration ttl = resolveBlacklistTtl(claims.expireAtMillis(), fallbackTtl);
        try {
            stringRedisTemplate.opsForValue().set(RedisKeys.authTokenBlacklist(token), "1", ttl);
        } catch (Exception ex) {
            log.error("Failed to blacklist token, userId={}", userId, ex);
            throw serverError("退出登录失败，请稍后重试");
        }
    }

    @Override
    public long incrementTokenVersion(Long userId) {
        try {
            Long version = stringRedisTemplate.opsForValue().increment(RedisKeys.authTokenVersion(userId));
            if (version == null) {
                throw serverError("登录态刷新失败");
            }
            return version;
        } catch (CommonException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to increment token version, userId={}", userId, ex);
            throw serverError("登录态刷新失败");
        }
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            throw new UnauthorizedException("未登录或登录已失效");
        }
        String normalized = authorization.trim();
        if (!normalized.regionMatches(true, 0, AUTHORIZATION_PREFIX, 0, AUTHORIZATION_PREFIX.length())) {
            throw new UnauthorizedException("无效的token");
        }
        String token = normalized.substring(AUTHORIZATION_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("无效的token");
        }
        return token;
    }

    private Duration resolveBlacklistTtl(Long expireAtMillis, Duration fallbackTtl) {
        if (expireAtMillis != null) {
            long remainMillis = expireAtMillis - System.currentTimeMillis();
            if (remainMillis > 0) {
                return Duration.ofMillis(remainMillis);
            }
        }
        if (fallbackTtl != null && !fallbackTtl.isNegative() && !fallbackTtl.isZero()) {
            return fallbackTtl;
        }
        return Duration.ofMinutes(1);
    }

    private CommonException serverError(String message) {
        return new CommonException(message, ResultCode.SERVER_ERROR.getCode());
    }
}
