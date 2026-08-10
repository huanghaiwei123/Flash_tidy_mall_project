package com.gdou.inteceptor;

import com.gdou.util.JwtUtil;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {
    private JwtUtil jwtUtil;
    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)){
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null) {
            log.warn("JWT 缺失，拦截请求: {}", request.getRequestURI());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\"}");
            return false;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
////        压测通道
//        if(token.startsWith("X-userId ")){
//            String uid = token.substring("X-userId ".length());
//            UserHolder.set(Long.valueOf(uid));
//            return true;
//        }

        if (!jwtUtil.verifyToken(token)) {
            log.warn("JWT 校验失败或已过期，拦截请求: {}", request.getRequestURI());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"登录已过期，请重新登录\"}");
            return false;
        }
        String userId = jwtUtil.getUserId(token);
        UserHolder.set(Long.valueOf(userId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.remove();
    }
}
