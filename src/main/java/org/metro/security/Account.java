package org.metro.security;

import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.Set;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Data
@ToString
public class Account {

    private String uid;
    private String name;
    private String password;
    private String salt;
    private Date createdTime;
    private Date updatedTime;

    private Set<String> roles;
    private Set<String> permissions;

}
