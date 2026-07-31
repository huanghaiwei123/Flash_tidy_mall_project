package com.gdou.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String signType = "RSA2";
    private String charset = "UTF-8";
    private String format = "json";
}

