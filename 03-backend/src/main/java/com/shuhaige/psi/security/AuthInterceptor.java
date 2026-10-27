package com.shuhaige.psi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器：校验 Authorization: Bearer <token>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            LoginUser user = jwtUtil.parse(token);
            if (user != null) {
                // 会员（读者端）token 只允许访问 /reader 接口，防止越权访问管理端数据
                if (user.isCustomer() && !request.getServletPath().startsWith("/reader/")) {
                    writeNoPermission(response);
                    return false;
                }
                UserContext.set(user);
                return true;
            }
        }
        writeUnauthorized(response);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                Result.fail(ErrorCode.UNAUTHORIZED, "未登录或登录已过期"));
    }

    private void writeNoPermission(HttpServletResponse response) throws Exception {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                Result.fail(ErrorCode.NO_PERMISSION, "当前身份无权访问该接口"));
    }

    private void write(HttpServletResponse response, int status, Result<Void> body) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}