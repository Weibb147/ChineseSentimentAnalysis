export const handleApiError = (error, message = '操作失败') => {
    console.error('错误详情:', error); // 控制台输出完整错误堆栈
    if (error.response) {
        // 后端返回的错误信息
        console.error('接口错误响应:', error.response.data);
        ElMessage.error(error.response.data.message || message);
    } else if (error.request) {
        // 请求未收到响应
        console.error('请求未响应:', error.request);
        ElMessage.error('网络请求失败，请检查您的网络连接或稍后重试');
    } else {
        // 其他错误
        console.error('未知错误:', error.message);
        ElMessage.error(message);
    }
};
