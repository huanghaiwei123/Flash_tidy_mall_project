package com.gdou.exception;

import lombok.Data;

/**
 * 用于业务异常
 */
@Data
public class BusinessException extends RuntimeException {
    private Integer code;
    public BusinessException(String message) {
        super(message);
    }
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    public BusinessException(String message,Integer code) {
        super(message);
        this.code = code;
    }
}
