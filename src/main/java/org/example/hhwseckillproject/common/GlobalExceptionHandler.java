package org.example.hhwseckillproject.common;

import org.example.hhwseckillproject.exception.RepeatKillException;
import org.example.hhwseckillproject.exception.SeckillCloseException;
import org.example.hhwseckillproject.exception.SeckillException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 秒杀关闭（未到时间或已结束） */
    @ExceptionHandler(SeckillCloseException.class)
    public Result<Void> handleSeckillClose(SeckillCloseException e) {
        return Result.seckillClosed(e.getMessage());
    }

    /** 重复秒杀 */
    @ExceptionHandler(RepeatKillException.class)
    public Result<Void> handleRepeatKill(RepeatKillException e) {
        return Result.repeatKill(e.getMessage());
    }

    /** 秒杀通用异常（兜底） */
    @ExceptionHandler(SeckillException.class)
    public Result<Void> handleSeckill(SeckillException e) {
        return Result.fail(e.getMessage());
    }

    /** 兜底：其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.fail("服务器内部错误: " + e.getMessage());
    }
}
