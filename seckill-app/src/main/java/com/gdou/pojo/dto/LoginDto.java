package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class LoginDto {
    @NotBlank
    @Pattern(regexp = "^(?:(?:\\+|00)86)?1\\d{10}$",message = "手机号格式不正确")
    private String phone;
    @NotBlank
    private String password;
}
