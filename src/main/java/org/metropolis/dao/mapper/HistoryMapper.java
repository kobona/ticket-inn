package org.metropolis.dao.mapper;

import org.metropolis.dao.model.History;

public interface HistoryMapper {
    int insert(History record);

    int insertSelective(History record);
}