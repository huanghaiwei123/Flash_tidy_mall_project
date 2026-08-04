package com.gdou.captcha.service;

import com.gdou.captcha.pojo.CaptchaVO;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class CaptchaService {

    public CaptchaVO generate() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        captcha.setLen(3); // 两个数运算
        String answer = captcha.text();  // 计算结果
        String expression = captcha.getArithmeticString(); // 如 3+5=?
        String image = Base64.getEncoder().encodeToString(captcha.toBase64().getBytes());
        return new CaptchaVO(answer, "data:image/png;base64," + image, expression);
    }
}
