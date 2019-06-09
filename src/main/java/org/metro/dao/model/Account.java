package org.metro.dao.model;

import lombok.Data;

import java.util.Date;

@Data
public class Account {

    private String uid;

    private String username;

    private String password;

    private String salt;

    private Date addTime;

    private Date modTime;

}