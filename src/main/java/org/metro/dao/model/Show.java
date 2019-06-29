package org.metro.dao.model;

import lombok.Data;

import java.util.Date;

@Data
public class Show {
    private Integer showId;

    private String showName;

    private Integer startTime;

    private Date createDate;

    private Date updateDate;

}