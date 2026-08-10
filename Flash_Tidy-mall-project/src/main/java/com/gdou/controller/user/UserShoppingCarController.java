package com.gdou.controller.user;

import com.gdou.common.Result;
import com.gdou.service.ShoppingCarService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 — 购物车
 */
@RestController
@RequestMapping("/hhw/user/cart")
@Slf4j
public class UserShoppingCarController {

    @Autowired
    private ShoppingCarService shoppingCarService;

    /**
     * 添加商品到购物车
     */
    @PostMapping
    public Result add(@RequestParam Long spuId,
                      @RequestParam Long skuId,
                      @RequestParam(defaultValue = "1") Integer quantity) {
        Long userId = UserHolder.get();
        return shoppingCarService.addToCart(userId, spuId, skuId, quantity);
    }

    /**
     * 查看购物车
     */
    @GetMapping
    public Result list() {
        Long userId = UserHolder.get();
        return shoppingCarService.queryCart(userId);
    }

    /**
     * 查看购物车汇总（总价、总数）
     */
    @GetMapping("/summary")
    public Result summary() {
        Long userId = UserHolder.get();
        return shoppingCarService.cartSummary(userId);
    }

    /**
     * 更新商品数量
     */
    @PutMapping("/{cartId}/quantity")
    public Result updateQuantity(@PathVariable Long cartId,
                                 @RequestParam Integer quantity) {
        Long userId = UserHolder.get();
        return shoppingCarService.updateQuantity(userId, cartId, quantity);
    }

    /**
     * 切换勾选状态
     */
    @PutMapping("/{cartId}/selected")
    public Result toggleSelected(@PathVariable Long cartId,
                                 @RequestParam Integer selected) {
        Long userId = UserHolder.get();
        return shoppingCarService.updateSelected(userId, cartId, selected);
    }

    /**
     * 全选/取消全选
     */
    @PutMapping("/select-all")
    public Result selectAll(@RequestParam Integer selected) {
        Long userId = UserHolder.get();
        return shoppingCarService.selectAll(userId, selected);
    }

    /**
     * 删除购物车商品
     */
    @DeleteMapping("/{cartId}")
    public Result remove(@PathVariable Long cartId) {
        Long userId = UserHolder.get();
        return shoppingCarService.removeFromCart(userId, cartId);
    }
}
