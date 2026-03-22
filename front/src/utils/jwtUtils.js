// src/utils/jwtUtils.js
export function parseJwt(token) {
    if (!token) {
        return {};
    }

    try {
        // 检查token格式
        const parts = token.split('.');
        if (parts.length !== 3) {
            console.warn('Invalid JWT token format');
            return {};
        }

        const base64Url = parts[1];
        if (!base64Url) {
            return {};
        }

        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        const payload = JSON.parse(jsonPayload);
        // 根据实际JWT结构返回claims或整个payload
        return payload.claims || payload;
    } catch (e) {
        console.error('解析 JWT 失败:', e);
        return {};
    }
}

// 检查token是否过期
export function isTokenExpired(token) {
    if (!token) return true;

    try {
        const parts = token.split('.');
        if (parts.length !== 3) return true;

        const payload = JSON.parse(atob(parts[1]));
        const exp = payload.exp || payload.claims?.exp;
        if (!exp) return true;

        return Date.now() >= exp * 1000;
    } catch (e) {
        return true;
    }
}

// 获取token中的用户信息
export function getUserInfoFromToken(token) {
    if (!token) return {};

    try {
        const payload = parseJwt(token);
        return {
            id: payload.id || payload.userId || payload.sub,
            username: payload.username || payload.name || payload.sub,
            role: payload.role || payload.authorities,
            ...payload // 包含所有其他字段
        };
    } catch (e) {
        console.error('从token获取用户信息失败:', e);
        return {};
    }
}
