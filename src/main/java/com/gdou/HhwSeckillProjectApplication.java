package com.gdou;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.gdou.mapper")
@SpringBootApplication
public class HhwSeckillProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(HhwSeckillProjectApplication.class, args);
    }

}
