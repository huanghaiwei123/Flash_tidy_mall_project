package com.gdou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.gdou.mapper")
@EnableAsync
@EnableScheduling
public class FlashTidyMallProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashTidyMallProjectApplication.class, args);
    }
}
