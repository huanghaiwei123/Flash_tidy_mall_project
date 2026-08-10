package com.gdou.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordEncrypt {
    public static String encrypt(String password) {
        String hashpw = BCrypt.hashpw(password, BCrypt.gensalt());
        return hashpw;
    }
    public static boolean verify(String password, String hashpw) {
        return BCrypt.checkpw(password, hashpw);
    }
}
