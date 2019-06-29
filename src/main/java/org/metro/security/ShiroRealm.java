package org.metro.security;

import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
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

    {
        HashedCredentialsMatcher hashMatcher = new HashedCredentialsMatcher();
        hashMatcher.setHashAlgorithmName(Sha256Hash.ALGORITHM_NAME);
        hashMatcher.setStoredCredentialsHexEncoded(false);
        hashMatcher.setHashIterations(256);
        this.setCredentialsMatcher(hashMatcher);
    }

    public String addSalt(String password, String salt) {
        HashedCredentialsMatcher matcher = (HashedCredentialsMatcher) getCredentialsMatcher();
        SimpleHash hash = new SimpleHash(matcher.getHashAlgorithmName(), password, salt, matcher.getHashIterations());
        return hash.toBase64();
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        if (principals == null) {
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }
        return (ShiroAccount) getAvailablePrincipal(principals);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        if (username == null) {
            throw new AccountException("Null username are not allowed by this realm.");
        }

        Account account = accountMapper.selectByPrimaryKey(username);
        if (account == null) {
            throw new UnknownAccountException("No account found for admin [" + username + "]");
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(new ShiroAccount(account), account.getPassword(), getName());
        info.setCredentials(ByteSource.Util.bytes(Base64.decode(account.getPassword())));
        info.setCredentialsSalt(ByteSource.Util.bytes(account.getSalt()));
        return info;
    }
}
