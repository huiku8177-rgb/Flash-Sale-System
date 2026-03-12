package com.flashsale.orderservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author strive_qin
 * @version 1.0
 * @description Test
 * @date 2026/3/12 16:29
 */
@RestController
@RequestMapping("/order")
public class Test {
    @GetMapping("/test")
    public String test(@RequestHeader("X-User-Id") String userId) {
        return "current userId = " + userId;
    }
}
