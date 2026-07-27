package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SeckillUserDto {
    /** 手机号（登录账号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^(?:(?:\\+|00)86)?1\\d{10}$",message = "手机号格式不正确")
    private String phone;

    /** 昵称 */
    @NotBlank
    private String nickname;

    /** Bcrypt加密密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

}
