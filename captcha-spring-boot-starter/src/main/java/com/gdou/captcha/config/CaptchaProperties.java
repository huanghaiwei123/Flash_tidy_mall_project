package com.gdou.captcha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {
    /** 验证码宽度 */
    private int width = 130;
    /** 验证码高度 */
    private int height = 48;
    /** 验证码长度 */
    private int len = 3;

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int getLen() { return len; }
    public void setLen(int len) { this.len = len; }
}
