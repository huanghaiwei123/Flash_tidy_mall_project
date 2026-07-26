package org.example.hhwseckillproject.exception;

/**
 * 秒杀通用异常（运行时异常）
 */
public class SeckillException extends RuntimeException {

    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
