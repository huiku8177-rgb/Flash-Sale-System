package com.flashsale.authservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * @author strive_qin
 * @version 1.0
 * @description AuthServiceApplication
 * @date 2026/3/20 00:00
 */


@SpringBootApplication
@Slf4j
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        log.info("auth-service application started");
    }

}
