package com.gdou.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long seckillId;
    private String userId;
//    存储redis库存分桶的桶编号，方便库存回滚到特定桶
    private Integer bucketId;
    private Long orderId;
//    消息幂等处理，防止消费者ack前崩溃导致的重复投递造成的损失
    private String messageId;
}
