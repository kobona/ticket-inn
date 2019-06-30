package org.metropolis.dao.mapper;

import org.metropolis.dao.model.Account;

public interface AccountMapper {
    int deleteByPrimaryKey(String uid);

    int insert(Account record);

    int insertSelective(Account record);

    Account selectByPrimaryKey(String uid);

    int updateByPrimaryKeySelective(Account record);

    int updateByPrimaryKey(Account record);
}