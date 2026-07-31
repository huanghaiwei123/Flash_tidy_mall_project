package com.gdou.mq;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
@Data
@Builder
public class SeckillMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long seckillId;
    private String userId;
    private Integer bucketId;
    private Long orderId;
//    jackson反序列化需要无参构造
    public SeckillMessage() {}
    public SeckillMessage(Long seckillId, String userId, Integer bucketId,Long orderId) {
        this.seckillId = seckillId;
        this.userId = userId;
        this.bucketId = bucketId;
        this.orderId = orderId;
    }
}
