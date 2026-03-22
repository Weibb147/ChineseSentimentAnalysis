// src/api/user.js
import request from '@/utils/request.js'

// 用户注册
export const userRegisterService = (registerData) => {
    // 直接发送JSON数据，让axios自动设置Content-Type为application/json
    return request.post('/auth/register', registerData);
}

// 用户登录
export const userLoginService = (loginData) => {
    // 直接发送JSON数据，让axios自动设置Content-Type为application/json
    return request.post('/auth/login', loginData);
}

// 获取用户详细信息
export const userInfoService = () => {
    return request.get('/user/profile')
}

// 修改个人信息
export const userInfoUpdateService = (userInfoData) => {
    return request.put('/user/profile', userInfoData)
}

// 修改密码
export const userUpdatePasswordService = (passwordData) => {
    return request.put('/user/password', passwordData);
}

// 更新头像（使用已存在的URL字符串）
export const userUpdateAvatarService = (imageUrl) => {
    return request.put('/user/avatar', { imageUrl })
}

// 发送邮箱验证码
export const sendEmailCodeService = (email) => {
    return request.post('/auth/send-code', null, {
        params: { email }
    });
}
