package com.gdou.inteceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.mapper.RoleMapper;
import com.gdou.mapper.UserRoleMapper;
import com.gdou.pojo.entity.Role;
import com.gdou.pojo.entity.UserRole;
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
    private UserRoleMapper userRoleMapper;
    private RoleMapper roleMapper;
    public JwtInterceptor(JwtUtil jwtUtil, UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.jwtUtil = jwtUtil;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
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
        String uri = request.getRequestURI();
        if (uri.startsWith("/hhw/admin")) {
            if(!hasRole(userId,"ADMIN")){
                log.error("用户{}还不是管理员,请求拒绝",userId);
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"这个人还不是管理员,请求拒绝\"}");
                return false;
            }
        } else if (uri.startsWith("/hhw/merchant")) {
            if(!hasRole(userId,"MERCHANT")){
                log.error("用户{}还不是商家,请求拒绝",userId);
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"这个人还不是商家,请求拒绝\"}");
                return false;
            }
        }
        return true;
    }

    private boolean hasRole(String userId, String role) {
        LambdaQueryWrapper<UserRole> userRoleLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userRoleLambdaQueryWrapper.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(userRoleLambdaQueryWrapper);
        if(userRole == null){
            return false;
        }
        Long roleId = userRole.getRoleId();
        LambdaQueryWrapper<Role> roleLambdaQueryWrapper = new LambdaQueryWrapper<>();
        roleLambdaQueryWrapper.eq(Role::getId, roleId);
        Role role1 = roleMapper.selectOne(roleLambdaQueryWrapper);
        if(role1 == null){
            return false;
        }
        if(!role1.getCode().equals(role)){
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.remove();
    }
}
