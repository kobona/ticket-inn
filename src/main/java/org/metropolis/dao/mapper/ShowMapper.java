package org.metropolis.dao.mapper;

import org.metropolis.dao.model.Show;

public interface ShowMapper {
    int deleteByPrimaryKey(Integer showId);

    int insert(Show record);

    int insertSelective(Show record);

    Show selectByPrimaryKey(Integer showId);

    int updateByPrimaryKeySelective(Show record);

    int updateByPrimaryKey(Show record);
}