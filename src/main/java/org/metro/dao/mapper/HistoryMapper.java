package org.metro.dao.mapper;

import org.metro.dao.model.History;

public interface HistoryMapper {
    int insert(History record);

    int insertSelective(History record);
}