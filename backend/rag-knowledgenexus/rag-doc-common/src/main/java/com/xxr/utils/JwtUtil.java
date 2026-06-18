package com.xxr.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JwtUtil {

    // TOKEN的有效期一小时（S）
    private static final int TOKEN_TIME_OUT = 3_600;
    // 加密KEY（和原来完全一样）
    private static final String TOKEN_ENCRY_KEY = "MDk4ZjZiY2Q0NjIxZDM3M2NhZGU0ZTgzMjYyN2I0ZjY";
    // 最小刷新间隔(S)
    private static final int REFRESH_TIME = 300;

    // 生产ID
    public static String getToken(Long id) {
        Map<String, Object> claimMaps = new HashMap<>();
        claimMaps.put("id", id);
        long currentTime = System.currentTimeMillis();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(currentTime))
                .subject("system")
                .issuer("xxr")
                .audience().add("app").and()
                .compressWith(CompressionCodecs.GZIP)
                .signWith(generalKey(), Jwts.SIG.HS256)
                .expiration(new Date(currentTime + TOKEN_TIME_OUT * 1000L))
                .claims(claimMaps)
                .compact();
    }

    /**
     * 获取token中的claims信息
     */
    private static Jws<Claims> getJws(String token) {
        return Jwts.parser()
                .verifyWith(generalKey())
                .build()
                .parseSignedClaims(token);
    }

    /**
     * 获取payload body信息
     */
    public static Claims getClaimsBody(String token) {
        try {
            return getJws(token).getPayload();
        } catch (ExpiredJwtException e) {
            return null;
        }
    }

    /**
     * 获取header信息
     */
    public static JwsHeader getHeaderBody(String token) {
        return getJws(token).getHeader();
    }

    /**
     * 是否过期
     * -1：有效，未到刷新时间
     * 0：有效，需要刷新
     * 1：过期
     * 2：无效token
     */
    public static int verifyToken(Claims claims) {
        if (claims == null) {
            return 1;
        }
        try {
            if (claims.getExpiration().before(new Date())) {
                return 1;
            }
            // 需要自动刷新TOKEN
            if ((claims.getExpiration().getTime() - System.currentTimeMillis()) > REFRESH_TIME * 1000L) {
                return -1;
            } else {
                return 0;
            }
        } catch (ExpiredJwtException ex) {
            return 1;
        } catch (Exception e) {
            return 2;
        }
    }

    /**
     * 生成加密KEY（新版 JJWT 写法）
     */
    public static SecretKey generalKey() {
        byte[] encodedKey = Base64.getDecoder().decode(TOKEN_ENCRY_KEY);
        return Keys.hmacShaKeyFor(encodedKey);
    }

    // 调试用的main方法已移除
}