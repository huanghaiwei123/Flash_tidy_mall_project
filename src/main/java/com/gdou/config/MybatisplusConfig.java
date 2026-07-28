package com.gdou.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisplusConfig {
     @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
         MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
         interceptor.addInnerInterceptor(new PaginationInnerInterceptor());  //添加分页插件
         interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());  //添加乐观锁插件
         return interceptor;
     }
}
