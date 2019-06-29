package org.metro.dao.mapper;

import org.metro.dao.model.Account;

public interface AccountMapper {
    int deleteByPrimaryKey(String uid);

    int insert(Account record);

    int insertSelective(Account record);

    Account selectByPrimaryKey(String uid);

    int updateByPrimaryKeySelective(Account record);

    int updateByPrimaryKey(Account record);
}