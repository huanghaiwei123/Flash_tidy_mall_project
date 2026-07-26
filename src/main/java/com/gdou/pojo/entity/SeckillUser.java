package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀用户表
 */
@Data
@TableName("seckill_user")
public class SeckillUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    @NotNull(message = "订单id不能为空")
    private Long id;

    /** 手机号（登录账号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^(?:(?:\\+|00)86)?1\\d{10}$",message = "手机号不匹配")
    private String phone;

    /** 昵称 */
    private String nickname;

    /** MD5加密密码 */
    private String password;

    /** 密码盐值 */
    private String salt;

    /** 头像地址 */
    private String avatar;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
}
