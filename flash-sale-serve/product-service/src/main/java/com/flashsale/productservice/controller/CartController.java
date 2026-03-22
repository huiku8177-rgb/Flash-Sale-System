package com.flashsale.productservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.dto.CartItemAddDTO;
import com.flashsale.productservice.domain.dto.CartItemUpdateDTO;
import com.flashsale.productservice.domain.vo.CartItemVO;
import com.flashsale.productservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description CartController
 * @date 2026/3/22 00:00
 */
@Tag(name = "购物车", description = "普通商品购物车相关接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/product/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    /**
     * 查询当前登录用户的购物车商品列表。
     *
     * @param userId 用户ID
     * @return 购物车商品列表
     */
    @Operation(summary = "查询购物车列表")
    @GetMapping("/items")
    public Result<List<CartItemVO>> listCartItems(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return cartService.listCartItems(userId);
    }

    /**
     * 将普通商品加入购物车。
     *
     * @param userId 用户ID
     * @param requestDTO 加购参数
     * @return 最新购物车商品信息
     */
    @Operation(summary = "新增购物车商品")
    @PostMapping("/items")
    public Result<CartItemVO> addCartItem(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                          @Valid @RequestBody CartItemAddDTO requestDTO) {
        log.info("用户 {} 新增购物车商品，productId={}", userId, requestDTO.getProductId());
        return cartService.addCartItem(userId, requestDTO);
    }

    /**
     * 更新购物车商品数量或选中状态。
     *
     * @param userId 用户ID
     * @param id 购物车项ID
     * @param requestDTO 更新参数
     * @return 最新购物车商品信息
     */
    @Operation(summary = "更新购物车商品")
    @PutMapping("/items/{id}")
    public Result<CartItemVO> updateCartItem(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                             @Parameter(description = "购物车项ID", example = "1")
                                             @PathVariable("id") @Min(value = 1, message = "购物车项ID必须大于等于1") Long id,
                                             @Valid @RequestBody CartItemUpdateDTO requestDTO) {
        return cartService.updateCartItem(userId, id, requestDTO);
    }

    /**
     * 删除单个购物车商品。
     *
     * @param userId 用户ID
     * @param id 购物车项ID
     * @return 删除结果
     */
    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteCartItem(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                       @Parameter(description = "购物车项ID", example = "1")
                                       @PathVariable("id") @Min(value = 1, message = "购物车项ID必须大于等于1") Long id) {
        return cartService.deleteCartItem(userId, id);
    }

    /**
     * 清空购物车，可选择仅清空已勾选商品。
     *
     * @param userId 用户ID
     * @param selectedOnly 是否只清空选中商品
     * @return 清空结果
     */
    @Operation(summary = "清空购物车")
    @DeleteMapping("/items")
    public Result<Void> clearCart(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                  @Parameter(description = "是否只清空已选中的商品", example = "true")
                                  @RequestParam(value = "selectedOnly", required = false) Boolean selectedOnly) {
        return cartService.clearCart(userId, selectedOnly);
    }
}
