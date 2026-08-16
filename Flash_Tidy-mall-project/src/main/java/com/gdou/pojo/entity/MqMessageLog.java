package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MQ消息发送日志表（兜底重发）
 * @TableName mq_message_log
 * mq消息落库，防止消息发送失败后消息丢失无法处理
 */
@TableName(value = "mq_message_log")
@Data
public class MqMessageLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务消息ID，幂等用 */
    private String messageId;

    /** 交换机 */
    private String exchange;

    /** 路由键 */
    private String routingKey;

    /** 消息内容(JSON) */
    private String messageBody;

    /** 0=待发送 1=已发送 2=发送失败 */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}