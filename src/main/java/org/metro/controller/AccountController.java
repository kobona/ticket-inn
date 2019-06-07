package org.metro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@RestController("/account")
public class AccountController {

    @GetMapping("/login")
    public String login() {
        return "hello spring boot";
    }
}
