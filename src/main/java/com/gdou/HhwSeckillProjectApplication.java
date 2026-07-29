package com.gdou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.gdou.mapper")
@SpringBootApplication
@EnableScheduling //开启定时任务
public class HhwSeckillProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(HhwSeckillProjectApplication.class, args);
    }

}
