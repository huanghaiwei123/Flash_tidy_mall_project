package com.gdou.captcha;

import com.gdou.captcha.config.CaptchaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CaptchaProperties.class)
@ComponentScan(basePackageClasses = CaptchaAutoConfiguration.class)
public class CaptchaAutoConfiguration {
}
