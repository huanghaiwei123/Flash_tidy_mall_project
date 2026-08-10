package com.gdou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.gdou.mapper")
@EnableAsync
public class FlashTidyMallProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashTidyMallProjectApplication.class, args);
    }
}
