package com.flashsale.common.util;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.flashsale.common.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTool {

    private static final String CLAIM_USER = "user";
    private static final String CLAIM_VERSION = "ver";
    private static final String CLAIM_EXPIRE_AT_MS = "exp_ms";

    private final JWTSigner jwtSigner;

    public JwtTool(KeyPair keyPair) {
        this.jwtSigner = JWTSignerUtil.createSigner("rs256", keyPair);
    }

    public String createToken(Long userId, Duration ttl) {
        return createToken(userId, ttl, 0L);
    }

    public String createToken(Long userId, Duration ttl, long tokenVersion) {
        long expireAtMillis = System.currentTimeMillis() + ttl.toMillis();
        return JWT.create()
                .setPayload(CLAIM_USER, userId)
                .setPayload(CLAIM_VERSION, tokenVersion)
                .setPayload(CLAIM_EXPIRE_AT_MS, expireAtMillis)
                .setExpiresAt(new Date(expireAtMillis))
                .setSigner(jwtSigner)
                .sign();
    }

    public Long parseToken(String token) {
        return parseTokenClaims(token).userId();
    }

    public TokenClaims parseTokenClaims(String token) {
        JWT jwt = parseAndValidate(token);
        Long userId = parseLongClaim(jwt.getPayload(CLAIM_USER), true);
        Long tokenVersion = parseLongClaim(jwt.getPayload(CLAIM_VERSION), false);
        Long expireAtMillis = parseLongClaim(jwt.getPayload(CLAIM_EXPIRE_AT_MS), false);
        return new TokenClaims(userId, tokenVersion == null ? 0L : tokenVersion, expireAtMillis);
    }

    private JWT parseAndValidate(String token) {
        if (token == null) {
            throw new UnauthorizedException("未登录");
        }

        JWT jwt;
        try {
            jwt = JWT.of(token).setSigner(jwtSigner);
        } catch (Exception e) {
            throw new UnauthorizedException("无效的token", e);
        }

        if (!jwt.verify()) {
            throw new UnauthorizedException("无效的token");
        }

        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new UnauthorizedException("token已经过期");
        }

        return jwt;
    }

    private Long parseLongClaim(Object value, boolean required) {
        if (value == null) {
            if (required) {
                throw new UnauthorizedException("无效的token");
            }
            return null;
        }

        try {
            return Long.valueOf(value.toString());
        } catch (RuntimeException ex) {
            if (required) {
                throw new UnauthorizedException("无效的token");
            }
            return null;
        }
    }

    public record TokenClaims(Long userId, long tokenVersion, Long expireAtMillis) {
    }
}
