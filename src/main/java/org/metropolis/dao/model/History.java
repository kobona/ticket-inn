package org.metropolis.dao.model;

import lombok.Data;

import java.util.Date;

@Data
public class History {
    private String uid;

    private Integer refId;

    private String opLog;

    private Date createDate;
}