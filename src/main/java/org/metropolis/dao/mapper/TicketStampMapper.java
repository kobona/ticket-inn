package org.metropolis.dao.mapper;

import org.metropolis.dao.model.TicketStamp;

public interface TicketStampMapper {
    int insert(TicketStamp record);

    int insertSelective(TicketStamp record);
}