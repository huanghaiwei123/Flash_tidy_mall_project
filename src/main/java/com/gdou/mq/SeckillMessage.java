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
//    jackson反序列化需要无参构造
    public SeckillMessage() {}
    public SeckillMessage(Long seckillId, String userId) {
        this.seckillId = seckillId;
        this.userId = userId;
    }
}
