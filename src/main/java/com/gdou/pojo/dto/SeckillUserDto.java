package com.gdou.pojo.dto;

import lombok.Data;

@Data
public class SeckillUserDto {
    /** 手机号（登录账号） */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** MD5加密密码 */
    private String password;

    /** 密码盐值 */
    private String salt;

    /** 头像地址 */
    private String avatar;
}
