package org.metro.test;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metro.TicketInnWebApp;
import org.metro.dao.mapper.AccountMapper;
import org.metro.dao.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@ActiveProfiles("dev")
@RunWith(SpringJUnit4ClassRunner.class)
@Import(TicketInnWebApp.class)
@SpringBootTest
public class MybatisTest {

    @Autowired
    AccountMapper accountMapper;

    @Test
    public void testMybatisWithSQLite() {

//        Account a = new Account();
//        a.setUid("root");
//        a.setUsername("root");
//        a.setPassword("123456");
//        a.setSalt("ABCDEFG");
//        a.setAddTime(new Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000));
//        a.setModTime(new Date());
//        accountMapper.insertSelective(a);

        Account admin = accountMapper.selectByPrimaryKey("root");
        Assert.assertEquals("root", admin.getUsername());
    }

}
