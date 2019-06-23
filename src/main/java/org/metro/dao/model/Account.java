package org.metro.dao.model;

import lombok.Data;

@Data
public class Account {
    private String uid;

    private String username;

    private String password;

    private String salt;

    private String roles;

    private String permissions;

    private Integer addDate;

    private Integer modDate;

}