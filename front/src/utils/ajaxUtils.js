/**
 * 封装的ajax get请求
 * @param url 请求url
 * @param params 请求参数
 * @param success 成功回调函数
 * @param error 失败回调函数
 * @param async 是否异步
 */
function ajaxGet(url, params, success, error, async = true) {
    $.ajax({
        type: "GET",
        url: url,
        data: params,
        cache: false,
        async: async,
        dataType: "json",
        processData: true,
        success: success,
        error: error
    });
}

/**
 * 封装的ajax post请求
 * @param url 请求url
 * @param params 请求参数
 * @param success 成功回调函数
 * @param error 失败回调函数
 * @param async 是否异步
 */
function ajaxPost(url, params, success, error, async = true) {
    $.ajax({
        type: "POST",
        url: url,
        data: params,
        async: async,
        cache: false,
        dataType: "json",
        processData: true,
        success: success,
        error: error
    });
}

/**
 * 错误回调函数
 * @param res
 */
let error = (res) => {
    let response = res.responseJSON;

    // 请求有响应
    if (res && response) {
        let status = res.status;

        if (status) {
            let message;

            if (status === 404 && response.path) {
                message = "路径" + response.path + "不存在。";
            } else {
                message = response.message;
            }

            alert(message);
            console.log("响应状态码：" + status + ", 响应消息：" + message);
        } else {
            console.log("请求没有响应状态码~");
        }
    } else {
        console.log("请求无响应~");
    }
}
