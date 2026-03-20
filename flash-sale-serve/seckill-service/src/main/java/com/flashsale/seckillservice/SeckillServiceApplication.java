package com.flashsale.seckillservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillServiceApplication
 * @date 2026/3/20 00:00
 */


@SpringBootApplication
@MapperScan("com.flashsale.seckillservice.mapper")
public class SeckillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApplication.class, args);
    }

}
