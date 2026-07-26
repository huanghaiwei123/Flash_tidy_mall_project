package org.example.hhwseckillproject.exception;

/**
 * 重复秒杀异常（同一用户对同一商品只能秒杀一次）
 */
public class RepeatKillException extends SeckillException {

    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
