package com.gdou.interceptor;
import com.gdou.limit.RateLimit;
import com.gdou.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource(name = "RateLimitScript")
    private DefaultRedisScript<Long> rateLimitScript;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        // 只拦截带有注解的控制器
        if (annotation == null) {
            return true;
        }
        String userId = UserHolder.getUserId();
        // key的组成为前缀+请求路径url+用户ID或ip地址
        String key = "Rate:" + request.getRequestURI() + (userId != null ? userId : request.getRemoteAddr());
        // 使用 StringRedisTemplate 确保 args 以纯字符串序列化，Lua 脚本 tonumber() 能正确解析
        Long count = stringRedisTemplate.execute(rateLimitScript, Arrays.asList(key),
                String.valueOf(annotation.limit()), String.valueOf(annotation.second()));
        if (count != null && count == -1) {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"code\":429,\"message\":\"请求太频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }
}
