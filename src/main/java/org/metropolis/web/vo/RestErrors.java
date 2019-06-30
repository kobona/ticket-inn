package org.metropolis.web.vo;

public enum RestErrors {

    BadRequest(400, "参数有误"),
    Unauthorized(401, "未登录"),
    Forbidden(403, "无权访问"),
    NotFound(404, "资源不存在"),
    UnsupportedMediaType(415, "不支持参数类型"),
    UnknownError(500, "系统异常");

    int code;
    String msg;
    RestErrors(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}