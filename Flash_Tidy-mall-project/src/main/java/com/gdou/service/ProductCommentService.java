package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.ProductCommentDto;

public interface ProductCommentService {
    /** 发表评论或回复（任意登录用户） */
    Result addComment(Long userId, ProductCommentDto dto);

    /** 商品评论列表（楼中楼，分页） */
    Result listBySpu(Long spuId, Integer page, Integer size);

    /** 我的评论列表 */
    Result listMyComments(Long userId);

    /** 商家回复评论 */
    Result replyComment(Long merchantId, Long commentId, String replyContent);

    /** 商家查看自家店铺商品的评论列表 */
    Result listMerchantComments(Long merchantId);
}