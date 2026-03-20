package com.flashsale.productservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderCheckoutDTO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "普通订单创建请求")
public class NormalOrderCheckoutDTO {

    @NotEmpty(message = "下单商品项不能为空")
    @Valid
    @Schema(description = "下单商品项")
    private List<NormalOrderItemRequestDTO> items;

    @Size(max = 200, message = "订单备注长度不能超过200个字符")
    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;

    @Size(max = 2000, message = "地址快照长度不能超过2000个字符")
    @Schema(
            description = "地址快照JSON字符串",
            example = "{\"receiver\":\"小曾\",\"phone\":\"13800000000\",\"address\":\"上海市浦东新区世纪大道100号\"}"
    )
    private String addressSnapshot;
}
