package org.metro.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
public class ShiroSecurityManager extends SessionsSecurityManager {

    @Override
    public Subject login(Subject subject, AuthenticationToken authenticationToken) throws AuthenticationException {
        return null;
    }

    @Override
    public void logout(Subject subject) {

    }

    @Override
    public Subject createSubject(SubjectContext subjectContext) {
        return null;
    }
}
