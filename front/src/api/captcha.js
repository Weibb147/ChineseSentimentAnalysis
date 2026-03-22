// src/api/captcha.js
import request from '@/utils/request.js'

// 获取验证码图片URL（直接用于<img src>，不要用axios请求）
export const getCaptchaImageUrl = (uuid) => {
    return `/api/captcha/generate?uuid=${uuid}`
}

// 验证码验证接口（暂时不需要，因为后端在登录时直接验证）
// export const verifyCaptcha = (captchaData) => {
//     return request.post('/captcha/verify', captchaData)
// }

// 刷新验证码接口（GET方式）
// export const refreshCaptcha = (uuid) => {
//     return request.get(`/captcha/refresh?uuid=${uuid}`)
// }
