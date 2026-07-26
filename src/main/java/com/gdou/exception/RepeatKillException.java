package com.gdou.exception;

/**
 * 定义一个重复下单异常类
 * 请勿重复下单
 */
public class RepeatKillException extends SeckillException {
    public RepeatKillException(String message) {
        super(message);
    }
    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
