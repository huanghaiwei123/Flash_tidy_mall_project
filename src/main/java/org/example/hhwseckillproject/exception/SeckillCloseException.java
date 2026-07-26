package org.example.hhwseckillproject.exception;

/**
 * 秒杀关闭异常（未到秒杀时间或已结束）
 */
public class SeckillCloseException extends SeckillException {

    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
