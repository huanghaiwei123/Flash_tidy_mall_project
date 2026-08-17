package com.gdou.config;

import com.gdou.inteceptor.JwtInterceptor;
import com.gdou.mapper.RoleMapper;
import com.gdou.mapper.UserRoleMapper;
import com.gdou.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(jwtUtil,userRoleMapper,roleMapper))
                .addPathPatterns("/**")
                .excludePathPatterns("/hhw/login"
                ,"/hhw/register"
                ,"/hhw/goods/**"
                ,"/hhw/comment/list/**"
                ,"/hhw/pay/notify"
                ,"/hhw/pay/return");
    }
}
