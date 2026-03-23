package com.flashsale.productservice.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description CreateNormalOrderRequestDTO
 * @date 2026/3/20 00:00
 */
@Data
public class CreateNormalOrderRequestDTO {

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private String remark;

    private String receiver;

    private String mobile;

    private String detail;

    private List<CreateNormalOrderItemDTO> items;
}
