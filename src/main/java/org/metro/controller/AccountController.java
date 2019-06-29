package org.metro.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.metro.web.vo.RestEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@RestController
@RequestMapping("/account")
public class AccountController {

    @PostMapping("/login")
    public RestEntity login(@RequestParam(required = false) String username,
                            @RequestParam(required = false) String password) {
        Subject subject = SecurityUtils.getSubject();
        if (! subject.isAuthenticated()) {
            UsernamePasswordToken authToken = new UsernamePasswordToken(username, password);
            subject.login(authToken);
        }
        String token = (String) subject.getSession(false).getId();
        return RestEntity.ok(token);
    }

    @GetMapping("/logout")
    public RestEntity logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            subject.logout();
        }
        return RestEntity.ok();
    }

    @GetMapping("/get")
    public String get() {
        return "hello spring boot";
    }
}
