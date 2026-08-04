package com.gdou.interceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
import com.gdou.util.JwtUtil;
import com.gdou.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

//        // 压测专用：通过 X-UserId 头直接指定用户，跳过 JWT（上线前删除此段）
//        String testUserId = request.getHeader("X-UserId");
//        if (testUserId != null && !testUserId.isEmpty()) {
//            request.setAttribute("userId", testUserId);
//            UserHolder.setUserId(testUserId);
//            return true;
//        }

        // 正常 JWT 流程
        String header = request.getHeader("Authorization");
        if (header == null || header.isEmpty()) {
            writeError("未登录请返回登录", ResultCodeConstant.LOGIN_ERROR, response);
            return false;
        }
        String token = header.startsWith("Bearer ") ? header.substring(7) : header;
        if (!jwtUtil.validateToken(token)) {
            writeError("token无效或已过期", ResultCodeConstant.NOT_LOGIN, response);
            return false;
        }
        String phone = jwtUtil.getPhone(token);
        String userId = jwtUtil.getUserId(token);
        String loginIp = jwtUtil.getIp(token);

        // IP 绑定校验：防止 token 被盗用
        String currentIp = getClientIp(request);
        if (loginIp != null && !loginIp.equals(currentIp)) {
            writeError("登录IP与当前IP不一致，请重新登录", ResultCodeConstant.LOGIN_ERROR, response);
            return false;
        }

        request.setAttribute("userId", userId);
        request.setAttribute("phone", phone);
        UserHolder.setUserId(userId);
        return true;
    }

    private void writeError(String msg, Integer code,HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8"); //设置响应内容类型为json
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); //401未授权
        Result result =Result.error(msg,code,null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 用于清理ThreadLocal中的内容
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.remove();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
