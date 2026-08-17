package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.common.Result;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.OrderItemMapper;
import com.gdou.mapper.OrderMapper;
import com.gdou.mapper.ProductCommentMapper;
import com.gdou.mapper.SpuMapper;
import com.gdou.mapper.UserMapper;
import com.gdou.pojo.dto.ProductCommentDto;
import com.gdou.pojo.entity.Order;
import com.gdou.pojo.entity.OrderItem;
import com.gdou.pojo.entity.ProductComment;
import com.gdou.pojo.entity.Spu;
import com.gdou.pojo.entity.User;
import com.gdou.service.ProductCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductCommentServiceImpl implements ProductCommentService {

    /** 允许标记"已购"的订单状态：已支付/已发货/已收货/已完成 */
    private static final List<String> PURCHASED_STATUS = Arrays.asList(
            ResultMsgConstant.STATUS_PAID,
            ResultMsgConstant.STATUS_SHIPPED,
            ResultMsgConstant.STATUS_RECEIVED,
            ResultMsgConstant.STATUS_COMPLETED);

    @Autowired
    private ProductCommentMapper productCommentMapper;
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result addComment(Long userId, ProductCommentDto dto) {
        // 1. 校验商品存在
        Spu spu = spuMapper.selectById(dto.getSpuId());
        if (spu == null) {
            throw new BusinessException("商品不存在");
        }

        ProductComment comment = new ProductComment();
        comment.setSpuId(dto.getSpuId());
        comment.setSkuId(dto.getSkuId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setImages(dto.getImages());
        comment.setStatus(1);
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());

        boolean isTop = dto.getParentId() == null;
        if (isTop) {
            // 顶级评论：评分可选
            comment.setParentId(null);
            comment.setCommentType("TOP");
            comment.setRating(dto.getRating());
            // 带了订单号则校验已购，通过才记录（展示"已购"标签）
            if (dto.getOrderNo() != null && !dto.getOrderNo().isEmpty()) {
                validatePurchased(userId, dto);
            }
            comment.setOrderNo(dto.getOrderNo());
        } else {
            // 回复：校验父评论存在且属于同一商品
            ProductComment parent = productCommentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("被回复的评论不存在");
            }
            if (!parent.getSpuId().equals(dto.getSpuId())) {
                throw new BusinessException("不能跨商品回复评论");
            }
            comment.setParentId(dto.getParentId());
            comment.setReplyUserId(parent.getUserId());
            // 商家在自家店铺回复，自动打上"商家"标签
            boolean isMerchant = spu.getMerchantId() != null && spu.getMerchantId().equals(userId);
            comment.setCommentType(isMerchant ? "MERCHANT_REPLY" : "USER_REPLY");
        }

        productCommentMapper.insert(comment);
        log.info("用户{}评论商品{}，类型{}", userId, dto.getSpuId(), comment.getCommentType());
        return Result.success("评论成功");
    }

    @Override
    public Result listBySpu(Long spuId, Integer page, Integer size) {
        // 1. 查顶级评论（分页）
        Page<ProductComment> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<ProductComment> topWrapper = new LambdaQueryWrapper<>();
        topWrapper.eq(ProductComment::getSpuId, spuId);
        topWrapper.eq(ProductComment::getStatus, 1);
        topWrapper.isNull(ProductComment::getParentId);
        topWrapper.orderByDesc(ProductComment::getCreateTime);
        productCommentMapper.selectPage(pageInfo, topWrapper);

        // 2. 批量查每个顶级评论下的回复
        List<ProductComment> topComments = pageInfo.getRecords();
        if (!topComments.isEmpty()) {
            List<Long> parentIds = topComments.stream()
                    .map(ProductComment::getId)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<ProductComment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.in(ProductComment::getParentId, parentIds);
            replyWrapper.eq(ProductComment::getStatus, 1);
            replyWrapper.orderByAsc(ProductComment::getCreateTime);
            List<ProductComment> replies = productCommentMapper.selectList(replyWrapper);

            Map<Long, List<ProductComment>> replyMap = replies.stream()
                    .collect(Collectors.groupingBy(ProductComment::getParentId));

            for (ProductComment top : topComments) {
                top.setReplies(replyMap.getOrDefault(top.getId(), java.util.Collections.emptyList()));
            }
        }
        // 填充用户昵称
        fillUserNames(topComments);
        return Result.success(pageInfo);
    }

    @Override
    public Result listMyComments(Long userId) {
        LambdaQueryWrapper<ProductComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductComment::getUserId, userId);
        wrapper.orderByDesc(ProductComment::getCreateTime);
        List<ProductComment> list = productCommentMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Override
    public Result replyComment(Long merchantId, Long commentId, String replyContent) {
        ProductComment comment = productCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        // 校验商品归属商家
        Spu spu = spuMapper.selectById(comment.getSpuId());
        if (spu == null || !merchantId.equals(spu.getMerchantId())) {
            throw new BusinessException("无权回复该评论");
        }
        // 商家回复作为一条楼中楼记录
        ProductComment reply = new ProductComment();
        reply.setSpuId(comment.getSpuId());
        reply.setUserId(merchantId);
        reply.setParentId(comment.getId());
        reply.setReplyUserId(comment.getUserId());
        reply.setCommentType("MERCHANT_REPLY");
        reply.setContent(replyContent);
        reply.setStatus(1);
        reply.setCreateTime(new Date());
        reply.setUpdateTime(new Date());
        productCommentMapper.insert(reply);
        return Result.success("回复成功");
    }

    @Override
    public Result listMerchantComments(Long merchantId) {
        // 1. 查商家所有 SPU
        LambdaQueryWrapper<Spu> spuWrapper = new LambdaQueryWrapper<>();
        spuWrapper.eq(Spu::getMerchantId, merchantId);
        spuWrapper.eq(Spu::getStatus, 1);
        List<Spu> spus = spuMapper.selectList(spuWrapper);
        if (spus.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }
        List<Long> spuIds = spus.stream().map(Spu::getId).collect(Collectors.toList());

        // 2. 查这些 SPU 的顶级评论
        LambdaQueryWrapper<ProductComment> topWrapper = new LambdaQueryWrapper<>();
        topWrapper.in(ProductComment::getSpuId, spuIds);
        topWrapper.eq(ProductComment::getStatus, 1);
        topWrapper.isNull(ProductComment::getParentId);
        topWrapper.orderByDesc(ProductComment::getCreateTime);
        List<ProductComment> topComments = productCommentMapper.selectList(topWrapper);
        if (topComments.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        // 3. 查每个顶级评论的回复
        List<Long> parentIds = topComments.stream().map(ProductComment::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ProductComment> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.in(ProductComment::getParentId, parentIds);
        replyWrapper.eq(ProductComment::getStatus, 1);
        replyWrapper.orderByAsc(ProductComment::getCreateTime);
        List<ProductComment> replies = productCommentMapper.selectList(replyWrapper);
        Map<Long, List<ProductComment>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(ProductComment::getParentId));
        for (ProductComment top : topComments) {
            top.setReplies(replyMap.getOrDefault(top.getId(), java.util.Collections.emptyList()));
        }

        // 4. 填充昵称
        fillUserNames(topComments);

        // 5. 附上商品名，方便商家看是哪条商品
        Map<Long, String> spuNameMap = spus.stream()
                .collect(Collectors.toMap(Spu::getId, Spu::getName, (a, b) -> a));
        for (ProductComment c : topComments) {
            c.setSpuName(spuNameMap.getOrDefault(c.getSpuId(), ""));
        }
        return Result.success(topComments);
    }

    /**
     * 给评论列表填充用户昵称（含顶级评论和回复）
     */
    private void fillUserNames(List<ProductComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        // 收集所有 userId（评论人 + 被回复人）
        Set<Long> userIds = new HashSet<>();
        for (ProductComment c : comments) {
            if (c.getUserId() != null) userIds.add(c.getUserId());
            if (c.getReplyUserId() != null) userIds.add(c.getReplyUserId());
        }
        if (userIds.isEmpty()) {
            return;
        }
        // 批量查用户昵称
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, userIds);
        Map<Long, String> nicknameMap = userMapper.selectList(userWrapper).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null ? u.getNickname() : u.getUsername(),
                        (a, b) -> a));
        // 填充顶级评论
        for (ProductComment c : comments) {
            c.setUserName(nicknameMap.getOrDefault(c.getUserId(), String.valueOf(c.getUserId())));
            c.setReplyUserName(nicknameMap.getOrDefault(c.getReplyUserId(), String.valueOf(c.getReplyUserId() == null ? "" : c.getReplyUserId())));
            // 填充回复
            if (c.getReplies() != null) {
                for (ProductComment r : c.getReplies()) {
                    r.setUserName(nicknameMap.getOrDefault(r.getUserId(), String.valueOf(r.getUserId())));
                    r.setReplyUserName(nicknameMap.getOrDefault(r.getReplyUserId(), String.valueOf(r.getReplyUserId() == null ? "" : r.getReplyUserId())));
                }
            }
        }
    }

    /**
     * 校验用户确实购买了该商品（订单号属于该用户且已支付/已收货，且包含该商品）
     */
    private void validatePurchased(Long userId, ProductCommentDto dto) {
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getOrderNo, dto.getOrderNo());
        orderWrapper.eq(Order::getUserId, userId);
        orderWrapper.in(Order::getStatus, PURCHASED_STATUS);
        Order order = orderMapper.selectOne(orderWrapper);
        if (order == null) {
            throw new BusinessException("订单不存在或不属于当前用户");
        }
        // 校验订单包含该商品
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderNo, dto.getOrderNo());
        itemWrapper.eq(OrderItem::getSpuId, dto.getSpuId());
        Long count = orderItemMapper.selectCount(itemWrapper);
        if (count == null || count == 0) {
            throw new BusinessException("该订单不包含此商品");
        }
    }
}