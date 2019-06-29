package org.metro.dao.mapper;

import org.metro.dao.model.Show;

public interface ShowMapper {
    int deleteByPrimaryKey(Integer showId);

    int insert(Show record);

    int insertSelective(Show record);

    Show selectByPrimaryKey(Integer showId);

    int updateByPrimaryKeySelective(Show record);

    int updateByPrimaryKey(Show record);
}