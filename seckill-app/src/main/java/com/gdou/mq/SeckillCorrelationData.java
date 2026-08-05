package com.gdou.mq;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
@Data
@NoArgsConstructor
public class SeckillCorrelationData extends CorrelationData {
    private SeckillMessage seckillMessage;
    public SeckillCorrelationData(String messageId,SeckillMessage seckillMessage) {
        super(messageId);
        this.seckillMessage = seckillMessage;
    }
}
