package com.gdou.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {
    @Bean
    public AlipayClient alipayClient(AlipayProperties props){
        return new DefaultAlipayClient(
                props.getGatewayUrl(),
                props.getAppId(),
                props.getMerchantPrivateKey(),
                props.getFormat(),
                props.getCharset(),
                props.getAlipayPublicKey(),
                props.getSignType()
        );
    }
}
