package com.flashsale.seckillservice.domain.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {

    private String name;

    private Integer status;

    private Long categoryId;
}
