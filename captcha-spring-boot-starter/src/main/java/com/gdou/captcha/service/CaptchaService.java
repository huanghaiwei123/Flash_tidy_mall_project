package com.gdou.captcha.service;

import com.gdou.captcha.config.CaptchaProperties;
import com.gdou.captcha.pojo.CaptchaVO;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {
    @Autowired
    private CaptchaProperties captchaProperties;
    public CaptchaVO generate() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(captchaProperties.getWidth(), captchaProperties.getHeight());
        captcha.setLen(captchaProperties.getLen());
        String answer = captcha.text();  // 计算结果
        String expression = captcha.getArithmeticString(); // 如 3+5=?
        // toBase64() 已返回完整 data URI，无需再编码和加前缀
        String image = captcha.toBase64();
        return new CaptchaVO(answer, image, expression);
    }
}
