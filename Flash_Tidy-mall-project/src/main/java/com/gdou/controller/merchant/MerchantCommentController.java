package com.gdou.controller.merchant;

import com.gdou.common.Result;
import com.gdou.service.ProductCommentService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/hhw/merchant/comment")
@Slf4j
public class MerchantCommentController {

    @Autowired
    private ProductCommentService productCommentService;

    /**
     * 商家查看自家店铺商品的评论列表
     */
    @GetMapping("/list")
    public Result list() {
        Long merchantId = UserHolder.get();
        return productCommentService.listMerchantComments(merchantId);
    }

    /**
     * 商家回复评论
     */
    @PostMapping("/reply")
    public Result reply(@RequestBody Map<String, Object> body) {
        Long merchantId = UserHolder.get();
        Long commentId = Long.valueOf(body.get("commentId").toString());
        String replyContent = (String) body.get("replyContent");
        log.info("商家{}回复评论{}", merchantId, commentId);
        return productCommentService.replyComment(merchantId, commentId, replyContent);
    }
}