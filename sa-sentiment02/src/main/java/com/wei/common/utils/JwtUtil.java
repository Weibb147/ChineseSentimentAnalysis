package com.wei.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Map;

/**
 * @author: admin
 * @date: 2024/4/15
 */
public class JwtUtil {
    private static final String KEY = "wei";

    //接受业务数据，生成token令牌并返回
    public static String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))
                .sign(Algorithm.HMAC256(KEY));
    }

    //接受token，验证token,并返回数据呼唤
    public static Map<String, Object> parseToken(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token);
        
        Claim claimsClaim = decodedJWT.getClaim("claims");
        return claimsClaim.asMap();
    }
}