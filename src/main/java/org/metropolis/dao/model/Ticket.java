package org.metropolis.dao.model;

import lombok.Data;

import java.util.Date;

@Data
public class Ticket {
    private Integer ticketId;

    private Integer showId;

    private String ticketName;

    private Integer ticketState;

    private Date createDate;

    private Date updateDate;

}