package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

@Data
public class UserDto {
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64, message = "用户名长度 2-64 位")
    private String username;

    /**
     * 密码（BCrypt 加密）
     */
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 昵称
     */
    @Size(max = 64, message = "昵称最长 64 位")
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 性别：0=未知 1=男 2=女
     */
    private Integer gender;

    /**
     * 生日
     */
    private Date birthday;
}
