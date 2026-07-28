package com.gdou.exception;

import com.gdou.Constant.ResultCodeConstant;
import com.gdou.Constant.ResultMessageConstant;
import com.gdou.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandle {
    /**
     * 业务异常
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public Result businessException(BusinessException e) {
        log.error(e.getMessage());
        return Result.error(e.getMessage(),e.getCode(),null);
    }

    /**
     * 参数校验异常
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("参数校验失败");
        log.error("参数校验异常:{}", errorMsg);
        return Result.error(errorMsg,ResultCodeConstant.ERROR,null);
    }

    /**
     * 全局异常兜底
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error(e.getMessage());
        return Result.error(e.getMessage(), ResultCodeConstant.ERROR,null);
    }
}
