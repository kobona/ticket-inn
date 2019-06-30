package org.metropolis.web.vo;

import org.springframework.http.HttpStatus;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
public class RestException extends RuntimeException {

    private int error;
    private HttpStatus httpStatus;

    public RestException(RestErrors error) {
        this(null, error.code, error.msg);
    }

    public RestException(HttpStatus httpStatus, RestErrors error) {
        this(httpStatus, error.code, error.msg);
    }

    public RestException(int error, String message) {
        this(null, error, message);
    }

    public RestException(HttpStatus httpStatus, int error, String message) {
        super(message);
        this.error = error;
        this.httpStatus = httpStatus;
    }

    @Override public Throwable fillInStackTrace() {
        return this;
    }

    public int error() {
        return error;
    }

    public HttpStatus httpStatus() { return httpStatus; }

}
