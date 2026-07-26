package org.example.hhwseckillproject.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 返回数据 */
    private T data;

    // ========== 静态工厂方法 ==========

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    // ========== 常用快捷方法 ==========

    public static <T> Result<T> error(String message) {
        return fail(message);
    }

    /** 秒杀关闭 */
    public static <T> Result<T> seckillClosed(String message) {
        return fail(601, message);
    }

    /** 重复秒杀 */
    public static <T> Result<T> repeatKill(String message) {
        return fail(602, message);
    }

    /** 库存不足 */
    public static <T> Result<T> soldOut(String message) {
        return fail(603, message);
    }
}
