package com.gdou.exception;

/**
 * 统一定义一个个跟秒杀业务相关的异常父类
 * 控制层可以 @ExceptionHandler统一捕获，也可以分类捕获
 */
public class SeckillException extends RuntimeException {
    public SeckillException(String message) {
        super(message);
    }
    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
