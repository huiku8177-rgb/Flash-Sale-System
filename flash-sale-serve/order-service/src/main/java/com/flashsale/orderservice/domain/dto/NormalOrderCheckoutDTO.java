package com.flashsale.orderservice.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 普通商品结算请求参数。
 */
@Data
public class NormalOrderCheckoutDTO {

    /**
     * 待下单商品列表。
     */
    private List<NormalOrderItemRequestDTO> items;

    /**
     * 订单备注。
     */
    private String remark;

    /**
     * 地址快照，建议前端传 JSON 字符串。
     */
    private String addressSnapshot;
}
