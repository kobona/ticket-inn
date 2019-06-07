package org.metro.web;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.metro.vo.RestEntity;
import org.metro.vo.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public RestEntity defaultExceptionHandler(
            HttpServletRequest request,  HttpServletRequest response, Exception e) {
        LOG.error("服务器异常 [{}]", request.getHeader("Authorization"), e);
        return RestEntity.error(RestEntity.StandardErrors.UnknownError);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestEntity handleMethodArgumentNotValidException(HttpServletRequest request,
                                                            HttpServletRequest response, MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        String message = bindingResult.getFieldErrors().stream().
                map(e -> {
                    String msg = e.getDefaultMessage();
                    if (!StringUtils.contains(msg, e.getField())) {
                        msg = e.getField() + " " + msg;
                    }
                    return msg;
                }).collect(Collectors.joining("；"));
        LOG.error("参数异常 [{}] {}", request.getHeader("Authorization"), message);
        return RestEntity.error(RestEntity.StandardErrors.BadRequest);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RestEntity handleHttpMessageNotReadableException(HttpServletRequest request,
                                                            HttpServletRequest response, HttpMessageNotReadableException ex) {
        LOG.error("参数类型异常 [{}]", request.getHeader("Authorization"), ex);
        return RestEntity.error(RestEntity.StandardErrors.UnsupportedMediaType);
    }

    @ExceptionHandler(RestException.class)
    public ResponseEntity<RestEntity> handleRestException(
            HttpServletRequest request,
            HttpServletRequest response, RestException ex) {

        HttpStatus status = ex.httpStatus();
        if (status == null) {
            status = HttpStatus.OK;
        }

        RestEntity error = RestEntity.error(ex.error(), StringUtils.defaultString(ex.getMessage()));
        LOG.error("参数类型异常 [{}]", request.getHeader("Authorization"), ex);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseBody
    public RestEntity handleUnauthenticatedException(HttpServletRequest request,
                                                     HttpServletRequest response, UnauthenticatedException ex) {
        LOG.error("授权异常 [{}]", request.getHeader("Authorization"), ex);
        return RestEntity.error(RestEntity.StandardErrors.Unauthorized);
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseBody
    public RestEntity handleUnauthorizedException(HttpServletRequest request,
                                                  HttpServletRequest response, UnauthorizedException ex) {
        LOG.error("越权访问 [{}]", request.getHeader("Authorization"), ex);
        return RestEntity.error(RestEntity.StandardErrors.Forbidden);
    }


}
