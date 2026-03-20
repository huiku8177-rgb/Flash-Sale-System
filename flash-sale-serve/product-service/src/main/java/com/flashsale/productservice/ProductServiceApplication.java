package com.flashsale.productservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
/**
 * @author strive_qin
 * @version 1.0
 * @description ProductServiceApplication
 * @date 2026/3/20 00:00
 */


@SpringBootApplication
@MapperScan("com.flashsale.productservice.mapper")
@EnableFeignClients
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
