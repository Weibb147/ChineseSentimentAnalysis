package com.wei.common.interceptor;

import com.wei.common.utils.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 由于使用了Spring Security，这里不再需要手动检查认证状态
        // Spring Security已经处理了认证和角色检查

        // 如果代码执行到这里，说明用户已通过认证
        // 可以通过request.getAttribute()获取认证信息
        Object roleObj = request.getAttribute("userRole");

        if (roleObj == null) {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(ResultUtils.error("用户角色信息缺失").toString());
            return false;
        }

        String role = roleObj.toString();

        // 检查是否是管理员
        if (!"ADMIN".equals(role)) {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(ResultUtils.error("无权限").toString());
            return false;
        }
        return true;
    }
}
