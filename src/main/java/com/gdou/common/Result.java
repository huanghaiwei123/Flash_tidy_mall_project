package com.gdou.common;

import com.gdou.Constant.ResultCodeConstant;
import com.gdou.Constant.ResultMessageConstant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {
    String message;
    Integer code;
    T data;
    public static<T> Result<T> success(T data){
        return new Result(ResultMessageConstant.SUCCESS,ResultCodeConstant.SUCCESS,data);
    }
    public static<T> Result<T> success(){
        return  success(null);
    }
    public static<T> Result<T> fail(T data) {
        return new Result<>(ResultMessageConstant.FAIL, ResultCodeConstant.FAIL,data);
    }
    public static<T>  Result<T> fail() {
        return fail(null);
    }
    public static<T> Result error(String message,Integer code,T data) {
        return new Result(message,code,data);
    }

    /** 秒杀关闭 */
    public  static<T> Result<T> seckillClosed() {
        return error(ResultMessageConstant.SeckillClose,ResultCodeConstant.SeckillClose,null);
    }

    /** 重复秒杀 */
    public static<T>  Result<T> repeatKill() {
        return error(ResultMessageConstant.RepeatKil,ResultCodeConstant.RepeatKill, null);
    }

    /** 库存不足 */
    public static<T>  Result<T> soldOut() {
        return error(ResultMessageConstant.StockEmpty,ResultCodeConstant.StockEmpty,null);
    }
}
