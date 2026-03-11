package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author strive_qin
 * @version 1.0
 * @description TestController
 * @date 2026/3/11 12:09
 */
@RestController
@RequestMapping("/seckill")
public class TestController {

    @GetMapping("/test")
    public Result<String> test(){
        return Result.success("seckill ok");
    }

}
