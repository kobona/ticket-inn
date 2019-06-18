package org.metro.controller;

import org.metro.web.vo.RestEntity;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * <p> Created by pengshuolin on 2019/6/18
 * https://www.jianshu.com/p/b06584591086
 */
@Controller
public class DefaultErrorController implements ErrorController {

    @Override
    public String getErrorPath() {
        return "/error";
    }

    @RequestMapping("/error")
    @ResponseBody
    public RestEntity error(HttpServletRequest request) {
        return RestEntity.error(RestEntity.StandardErrors.NotFound);
    }

}
