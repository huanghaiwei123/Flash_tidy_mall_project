package com.gdou.config;

import com.gdou.inteceptor.JwtInterceptor;
import com.gdou.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JwtUtil jwtUtil;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(jwtUtil))
                .addPathPatterns("/**")
                .excludePathPatterns("/hhw/login"
                ,"/hhw/register"
                ,"/hhw/goods/**"
                ,"/hhw/pay/notify");
    }
}
