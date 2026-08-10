package com.gdou.mq;

import com.gdou.pojo.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MqOrderDelayMessage {
    //    用于消息幂等处理，防止消费者发送ack失败导致消息重复处理
    private String messageId;
    private OrderDto orderDto;
    private Long userId;
    private String orderNo;
}
