package org.metropolis.test;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metropolis.TicketInnWebApp;
import org.metropolis.dao.mapper.AccountMapper;
import org.metropolis.dao.model.Account;
import org.metropolis.security.ShiroRealm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

/**
 *
 */
@ActiveProfiles("dev")
@RunWith(SpringJUnit4ClassRunner.class)
@Import(TicketInnWebApp.class)
@SpringBootTest
public class MybatisTest {

    @Autowired
    ShiroRealm shiroRealm;

    @Autowired
    AccountMapper accountMapper;

    @Test
    public void insertAccount() {

        String password = "123456";
        String salt = "ABCDE";

        Account a = new Account();
        a.setUid("root");
        a.setUsername("root");
        a.setPassword(shiroRealm.addSalt(password, salt));
        a.setSalt(salt);
        a.setRoles("");
        a.setPermissions("");
        a.setCreateDate(new Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000));
        a.setUpdateDate(new Date());
        accountMapper.deleteByPrimaryKey("root");

        Account admin = accountMapper.selectByPrimaryKey("root");
        Assert.assertEquals("root", admin.getUsername());
    }

    @Test
    public void selectAccount() {
        Account admin = accountMapper.selectByPrimaryKey("root");
        Assert.assertEquals("root", admin.getUsername());
    }

}
