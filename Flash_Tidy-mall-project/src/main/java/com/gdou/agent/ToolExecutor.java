package com.gdou.agent;

import java.util.Map;

/**
 * 工具执行器，执行模型给的参数，执行逻辑，返回结果字符串
 */
@FunctionalInterface   //用来标记一个接口是函数式接口,就是有且只有一个抽象方法的接口
public interface ToolExecutor {
    /**
     * 执行工具
     * @param args 模型传的参数（如 {"keyword":"耳机"}）
     * @return 执行结果文本，会回填给模型继续推理
     */
    String execute(Map<String, Object> args);
}
