package org.metropolis.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.metropolis.dao.model.Account;

import java.io.Serializable;
import java.util.*;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Data
@ToString
@NoArgsConstructor
public class ShiroAccount implements AuthorizationInfo {

    private String uid;
    private String username;
    private List<String> roles;
    private List<String> permissions;

    @NoArgsConstructor
    public static class SortedList extends AbstractList<String> implements Serializable {
        private String[] elements;
        public SortedList(String str) {
            String[] elements = StringUtils.split(str, ',');
            Arrays.sort(elements);
        }
        public int size() {
            return elements == null ? 0 : elements.length;
        }
        public String get(int index) {
            return elements[index];
        }
        public boolean contains(Object o) {
            if (elements != null && o instanceof String)
                return Arrays.binarySearch(elements, o) >= 0;
            return false;
        }
    }

    public ShiroAccount(Account account) {
        uid = account.getUid();
        username = account.getUsername();
        roles = new SortedList(account.getRoles());
        permissions = new SortedList(account.getPermissions());
    }

    @Override
    public Collection<String> getStringPermissions() {
        return permissions;
    }

    @Override
    public Collection<Permission> getObjectPermissions() {
        return null;
    }
}
