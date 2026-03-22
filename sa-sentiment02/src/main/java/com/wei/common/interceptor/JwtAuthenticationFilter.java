package com.wei.common.interceptor;

import com.wei.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;

    public JwtAuthenticationFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        log.debug("JwtAuthenticationFilter processing request: {}", request.getRequestURI());

        // 对于公开接口，直接放行
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头中获取token
        String token = getTokenFromRequest(request);

        // 如果没有token，返回401错误
        if (!StringUtils.hasText(token)) {
            log.debug("No token found in request");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\",\"data\":null}");
            return;
        }

        // 验证token是否有效
        Map<String, Object> claimsMap = validateAndParseToken(token);
        if (claimsMap == null) {
            log.debug("Token validation failed");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"无效的Token\",\"data\":null}");
            return;
        }

        // 从token中解析用户信息
        try {
            log.debug("Token parsed successfully, claims: {}", claimsMap);
            
            String username = (String) claimsMap.get("username");
            String role = (String) claimsMap.get("role");
            Long userId = ((Number) claimsMap.get("id")).longValue();

            // 创建认证对象，将用户ID也存储到认证对象中
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            // 将用户ID存储到认证对象的details中
            authenticationToken.setDetails(userId);

            // 将认证信息存入安全上下文
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            // 将用户角色信息添加到request属性中，供拦截器使用
            request.setAttribute("userRole", role);

            log.debug("Authentication set in SecurityContext for user: {}, role: {}", username, role);
        } catch (Exception e) {
            log.error("解析token失败: ", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Token解析失败\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // 如果没有Bearer前缀，直接返回token（兼容你的curl请求）
        return bearerToken;
    }

    private Map<String, Object> validateAndParseToken(String token) {
        try {
            // 检查token是否在redis中存在（即未被注销）
            Boolean hasKey = stringRedisTemplate.hasKey("TOKEN:" + token);
            if (hasKey != null && hasKey) {
                // 验证token本身是否有效并解析
                return JwtUtil.parseToken(token);
            }
        } catch (Exception e) {
            log.error("验证token失败: ", e);
        }
        return null;
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/auth/login") ||
               uri.equals("/api/auth/register") ||
               uri.equals("/api/auth/send-code") ||
               uri.startsWith("/api/captcha/");
    }
}
