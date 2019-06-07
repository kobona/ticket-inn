package org.metro.security;

import org.metro.security.Account;
import org.springframework.stereotype.Service;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Service
public class AccountService {

    public Account findAccount(String id) {
        return new Account();
    }


}
