package com.gdou.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoder {
    /**
     * 明文加密
     * @param password
     * 使用bcrypt明文加密
     * 这里本来想试用MD5加盐的，但是被告知可以暴力破解，于是使用了bcrypt
     * @return
     */
    public String encode(String password) {
        String hashpw = BCrypt.hashpw(password, BCrypt.gensalt());
        return hashpw;
    }

    /**
     * 密码校验
     * @param password
     * @param hashpw
     * @return
     */
    public boolean check(String password, String hashpw) {
        return BCrypt.checkpw(password, hashpw);
    }
}
