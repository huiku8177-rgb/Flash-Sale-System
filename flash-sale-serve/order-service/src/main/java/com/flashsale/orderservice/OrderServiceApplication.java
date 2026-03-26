package com.flashsale.orderservice;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * @author strive_qin
 * @version 1.0
 * @description OrderServiceApplication
 * @date 2026/3/20 00:00
 */


@SpringBootApplication
@MapperScan("com.flashsale.orderservice.mapper")
@EnableScheduling
@EnableFeignClients
@Slf4j
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        log.info("order-service application started");
    }

}
