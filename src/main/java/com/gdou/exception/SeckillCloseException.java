package com.gdou.exception;

/**
 * 定义一个秒杀关闭异常类
 * 秒杀未开启
 */
public class SeckillCloseException extends SeckillException{
    public SeckillCloseException(String message) {
        super(message);
    }
    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
