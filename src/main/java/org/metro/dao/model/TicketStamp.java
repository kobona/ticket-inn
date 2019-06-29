package org.metro.dao.model;

import lombok.Data;

import java.util.Date;

@Data
public class TicketStamp {
    private String uid;

    private Integer ticketId;

    private String ticketName;

    private Date createDate;

    private Date updateDate;
}