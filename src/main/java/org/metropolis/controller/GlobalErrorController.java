package org.metropolis.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.metropolis.web.vo.RestEntity;
import org.metropolis.web.vo.RestErrors;
import org.metropolis.web.vo.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * <p> Created by pengshuolin on 2019/6/18
 * https://www.jianshu.com/p/b06584591086
 */
@Controller
@RestControllerAdvice
public class GlobalErrorController implements ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalErrorController.class);

    @Override
    public String getErrorPath() {
        return "/error";
    }

    @RequestMapping("/error")
    public @ResponseBody RestEntity error(HttpServletRequest request) {
        return RestEntity.error(RestErrors.NotFound);
    }

    @RequestMapping("/not-login")
    public @ResponseBody RestEntity notLogin(HttpServletRequest request) {
        return RestEntity.error(RestErrors.Unauthorized);
    }

    @RequestMapping("/no-auth")
    public @ResponseBody RestEntity noAuth(HttpServletRequest request) {
        return RestEntity.error(RestErrors.Forbidden);
    }

    @ExceptionHandler(Exception.class)
    public @ResponseBody RestEntity defaultExceptionHandler(
            HttpServletRequest request,  HttpServletRequest response, Exception e) {
        LOG.error("服务器异常 [{}]", request.getHeader(HttpHeaders.AUTHORIZATION), e);
        return RestEntity.error(RestErrors.UnknownError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public @ResponseBody RestEntity handleMethodArgumentNotValidException(HttpServletRequest request,
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
        LOG.error("参数异常 [{}] {}", request.getHeader(HttpHeaders.AUTHORIZATION), message);
        return RestEntity.error(RestErrors.BadRequest);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public @ResponseBody RestEntity handleHttpMessageNotReadableException(HttpServletRequest request,
                                                            HttpServletRequest response, HttpMessageNotReadableException ex) {
        LOG.error("参数类型异常 [{}]", request.getHeader(HttpHeaders.AUTHORIZATION), ex);
        return RestEntity.error(RestErrors.UnsupportedMediaType);
    }

    @ExceptionHandler(RestException.class)
    public @ResponseBody ResponseEntity<RestEntity> handleRestException(
            HttpServletRequest request,
            HttpServletRequest response, RestException ex) {

        HttpStatus status = ex.httpStatus();
        if (status == null) {
            status = HttpStatus.OK;
        }

        RestEntity error = RestEntity.error(ex.error(), StringUtils.defaultString(ex.getMessage()));
        LOG.error("参数类型异常 [{}]", request.getHeader(HttpHeaders.AUTHORIZATION), ex);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public @ResponseBody RestEntity handleAuthenticationException(HttpServletRequest request,
                                                                  HttpServletRequest response, AuthenticationException ex) {
        LOG.error("登录异常", ex);
        return RestEntity.error(RestErrors.BadRequest);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public @ResponseBody RestEntity handleUnauthenticatedException(HttpServletRequest request,
                                                     HttpServletRequest response, UnauthenticatedException ex) {
        LOG.error("授权异常 [{}]", request.getHeader(HttpHeaders.AUTHORIZATION), ex);
        return RestEntity.error(RestErrors.Unauthorized);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public @ResponseBody RestEntity handleUnauthorizedException(HttpServletRequest request,
                                                  HttpServletRequest response, UnauthorizedException ex) {
        LOG.error("越权访问 [{}]", request.getHeader(HttpHeaders.AUTHORIZATION), ex);
        return RestEntity.error(RestErrors.Forbidden);
    }

}
