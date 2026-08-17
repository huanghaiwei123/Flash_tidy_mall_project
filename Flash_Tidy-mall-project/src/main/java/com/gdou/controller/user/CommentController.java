package com.gdou.controller.user;

import com.gdou.common.Result;
import com.gdou.pojo.dto.ProductCommentDto;
import com.gdou.service.ProductCommentService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/hhw/comment")
@Slf4j
public class CommentController {

    @Autowired
    private ProductCommentService productCommentService;

    /**
     * 发表评论或回复（任意登录用户）
     */
    @PostMapping
    public Result add(@Valid @RequestBody ProductCommentDto dto) {
        Long userId = UserHolder.get();
        log.info("用户{}发表评论，spuId={}", userId, dto.getSpuId());
        return productCommentService.addComment(userId, dto);
    }

    /**
     * 商品评论列表（游客可看）
     */
    @GetMapping("/list/{spuId}")
    public Result list(@PathVariable Long spuId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size) {
        return productCommentService.listBySpu(spuId, page, size);
    }

    /**
     * 我的评论
     */
    @GetMapping("/my")
    public Result my() {
        Long userId = UserHolder.get();
        return productCommentService.listMyComments(userId);
    }
}