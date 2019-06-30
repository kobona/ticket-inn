package org.metropolis.web.vo;

import lombok.Data;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Data
public class RestEntity {

    private int error;
    private String message;
    private Object data;

    public static RestEntity ok() {
        RestEntity entity = new RestEntity();
        entity.error = 0;
        entity.data = null;
        return entity;
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


    public static RestEntity error(RestErrors error) {
        RestEntity entity = new RestEntity();
        entity.error = error.code;
        entity.message = error.msg;
        return entity;
    }
}
