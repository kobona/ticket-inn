package org.metropolis.dao.model;

import lombok.Data;

import java.util.Date;

@Data
public class Account {
    private String uid;

    private String username;

    private String password;

    private String salt;

    private String roles;

    private String permissions;

    private Date createDate;

    private Date updateDate;
}