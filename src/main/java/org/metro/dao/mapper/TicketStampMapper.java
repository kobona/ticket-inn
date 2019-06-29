package org.metro.dao.mapper;

import org.metro.dao.model.TicketStamp;

public interface TicketStampMapper {
    int insert(TicketStamp record);

    int insertSelective(TicketStamp record);
}