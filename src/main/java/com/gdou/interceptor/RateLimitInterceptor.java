package com.gdou.interceptor;
import com.gdou.limit.RateLimit;
import com.gdou.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Resource(name = "RateLimitScript")
    private DefaultRedisScript defaultRedisScript;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)){
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
//        只拦截带有注解的控制器
        if(annotation == null){
            return true;
        }
        String userId = UserHolder.getUserId();
//        key的组成为前缀+请求路径url+用户ID或ip地址
        String key="Rate:"+request.getRequestURI()+(userId!=null?userId:request.getRemoteAddr());
        Long count = (Long)redisTemplate.execute(defaultRedisScript, Arrays.asList(key), annotation.limit(), annotation.second());
//        设置过期时间
         if(count==-1){
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"code\":429,\"message\":\"请求太频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }
}
