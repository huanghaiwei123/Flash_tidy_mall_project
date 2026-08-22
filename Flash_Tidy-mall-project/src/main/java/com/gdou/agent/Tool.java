package com.gdou.agent;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 一个agent工具：模型可调用的函数
 */
@AllArgsConstructor
@Data
public class Tool {
    private final String name;  //工具名
    private final String description;  //给模型的说明，决定他何时调用
    private final Map<String,Object> parameters;   //参数json schema
    private final ToolExecutor toolExecutor;

    /**
     * 转成Ollama/OpenAi认识的格式
     * @return
     */
    public Map<String,Object> toSchema() {
        Map<String,Object> function = new HashMap<>();
        function.put("name",name);
        function.put("description",description);
        function.put("parameters",parameters);
        Map<String,Object> tool = new HashMap<>();
        tool.put("type","function");
        tool.put("function",function);
        return tool;
    }

    public String execute(Map<String,Object> args) {
        return toolExecutor.execute(args);
    }
}
