package org.metro.web.vo;

import lombok.Data;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Data
public class RestEntity {

    private int error;
    private String message;
    private Object data;

    public enum StandardErrors {
        BadRequest(400, "参数有误"),
        Unauthorized(401, "未登录"),
        Forbidden(403, "无权访问"),
        UnsupportedMediaType(415, "不支持参数类型"),
        UnknownError(500, "系统异常");
        private int code;
        private String msg;
        StandardErrors(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }
    }

    public static RestEntity ok(Object data) {
        RestEntity entity = new RestEntity();
        entity.error = 0;
        entity.data = data;
        return entity;
    }

    public static RestEntity error(String message) {
        RestEntity entity = new RestEntity();
        entity.error = -1;
        entity.message = message;
        return entity;
    }
    public static RestEntity error(int error, String message) {
        RestEntity entity = new RestEntity();
        entity.error = error;
        entity.message = message;
        return entity;
    }


    public static RestEntity error(StandardErrors error) {
        RestEntity entity = new RestEntity();
        entity.error = error.code;
        entity.message = error.msg;
        return entity;
    }
}
