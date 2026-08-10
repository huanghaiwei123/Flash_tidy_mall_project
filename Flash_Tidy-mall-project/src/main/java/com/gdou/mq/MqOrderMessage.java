package com.gdou.mq;

import com.gdou.pojo.dto.OrderDto;
import com.gdou.pojo.dto.ShoppingCarDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqOrderMessage {
//    用于消息幂等处理，防止消费者发送ack失败导致消息重复处理
    private String messageId;
    private OrderDto orderDto;
    private Long userId;
    private Long couponId;
    /** 收货地址快照 */
    private Object addressSnapshot;
}
