package com.gdou.common;

import com.gdou.constant.ResultCodeConstant;
import com.gdou.constant.ResultMsgConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeConstant.SUCCESS, ResultMsgConstant.SUCCESS,data);
    }
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCodeConstant.SUCCESS,msg,data);
    }
    public static <T> Result<T> success(String msg) {
        return new Result<>(ResultCodeConstant.SUCCESS,msg,null);
    }
    public static <T> Result<T> Fail(Integer code,String msg,T data) {
        return new Result<>(code,msg,data);
    }
    public static <T> Result<T> Fail(String msg,T data) {
        return new Result<>(ResultCodeConstant.FAIL,msg,data);
    }
    public static <T> Result<T> Fail(String msg) {
        return new Result<>(ResultCodeConstant.FAIL,msg,null);
    }
}
