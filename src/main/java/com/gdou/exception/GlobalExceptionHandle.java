package com.gdou.exception;

import com.gdou.Constant.ResultCodeConstant;
import com.gdou.Constant.ResultMessageConstant;
import com.gdou.common.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandle {
    @ExceptionHandler(value = RepeatKillException.class)
    public Result RepeatKillHandle() {
        return Result.repeatKill();
    }
    @ExceptionHandler(value= SeckillCloseException.class)
    public Result SeckillCloseHandle() {
        return Result.seckillClosed();
    }
    @ExceptionHandler(value= SeckillException.class)
    public Result SeckillHandle() {
        return Result.error(ResultMessageConstant.SeckillError, ResultCodeConstant.ERROR,null);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("参数校验失败");
        return Result.error(errorMsg,ResultCodeConstant.ERROR,null);
    }
}
