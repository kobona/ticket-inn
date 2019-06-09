package org.metro.security;

import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.metro.dao.mapper.AccountMapper;
import org.metro.dao.model.Account;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
public class ShiroRealm extends AuthorizingRealm {

    @Autowired
    private AccountMapper accountMapper;

    private ShiroAccount findAccount(String uid) {
        Account account = accountMapper.selectByPrimaryKey(uid);
        return account == null ? null: new ShiroAccount(account);
    }

    {
        HashedCredentialsMatcher hashMatcher = new HashedCredentialsMatcher();
        hashMatcher.setHashAlgorithmName(Sha256Hash.ALGORITHM_NAME);
        hashMatcher.setStoredCredentialsHexEncoded(false);
        hashMatcher.setHashIterations(1024);
        this.setCredentialsMatcher(hashMatcher);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        if (principals == null) {
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }
        ShiroAccount account = (ShiroAccount) getAvailablePrincipal(principals);
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setRoles(account.getRoles());
        info.setStringPermissions(account.getPermissions());
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        if (username == null) {
            throw new AccountException("Null username are not allowed by this realm.");
        }

        ShiroAccount account = findAccount(username);
        if (account == null) {
            throw new UnknownAccountException("No account found for admin [" + username + "]");
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(account, account.getPassword(), getName());
        if (account.getSalt() != null) {
            info.setCredentialsSalt(ByteSource.Util.bytes(account.getSalt()));
        }

        return info;
    }
}
