package com.gdou.config;
import com.gdou.interceptor.JwtInterceptor;
import com.gdou.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JwtInterceptor jwtInterceptor;
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/hhw/**")   //拦截所有业务接口
                .excludePathPatterns("/hhw/login"   //排除登录，注册
                ,"/hhw/register"
                ,"/hhw/seckill/seckills"  //查看秒杀商品列表
                ,"/hhw/pay/notify"       //支付宝异步回调
                ,"/hhw/pay/return");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/hhw/seckill/**");

    }
}
