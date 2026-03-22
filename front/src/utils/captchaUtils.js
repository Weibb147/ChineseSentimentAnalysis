// src/utils/captchaUtils.js
/**
 * 验证码工具函数
 */

// 生成随机字符串
export const generateRandomString = (length = 11) => {
  const charset = "abcdefghijklmnopqrstuvwxyz-_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  const values = new Uint32Array(length);
  window.crypto.getRandomValues(values);
  let str = "";
  for (let i = 0; i < length; i++) {
    str += charset[values[i] % charset.length];
  }
  return str;
};

// 获取验证码UUID
export const getCaptchaUuid = () => {
  return localStorage.getItem("login-captcha:uuid");
};

// 设置验证码UUID
export const setCaptchaUuid = (uuid) => {
  localStorage.setItem("login-captcha:uuid", uuid);
};

// 初始化验证码UUID
export const initCaptchaUuid = () => {
  let uuid = getCaptchaUuid();
  if (!uuid) {
    uuid = generateRandomString();
    setCaptchaUuid(uuid);
  }
  return uuid;
};

// 清除验证码UUID
export const clearCaptchaUuid = () => {
  localStorage.removeItem("login-captcha:uuid");
};