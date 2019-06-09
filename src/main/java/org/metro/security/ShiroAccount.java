package org.metro.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.metro.dao.model.Account;

import java.util.Date;
import java.util.Set;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Data
@ToString
@NoArgsConstructor
public class ShiroAccount extends Account {

    public ShiroAccount(Account account) {
        setUid(account.getUid());
        setUsername(account.getUsername());
        setPassword(account.getPassword());
        setSalt(account.getSalt());
        setAddTime(account.getAddTime());
        setModTime(account.getModTime());
    }

    private Set<String> roles;
    private Set<String> permissions;

}
