package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RegisterDto {
    /** 手机号（登录账号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^(?:(?:\\+|00)86)?1\\d{10}$",message = "手机号格式不正确")
    private String phone;

    /** 昵称 */
    @NotBlank
    private String nickname;

    /** Bcrypt加密密码 */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^\\S*(?=\\S{6,})(?=\\S*\\d)(?=\\S*[A-Z])(?=\\S*[a-z])(?=\\S*[!@#$%^&*? ])\\S*$"
            ,message = "密码至少为一个大写，一个小写，一个数字，一个特殊字符，长度至少六位")
    private String password;

}
