package com.shuhaige.psi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.service.UserPermService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 岗位权限校验拦截器。
 * <p>必须由 WebMvcConfig 以 order(2) 注册在 AuthInterceptor（order=1）之后，
 * 因为本拦截器依赖 UserContext 中的登录用户。
 * <p>未通过时直接写 HTTP 403（不能抛 BizException：全局异常处理器会把它转成 HTTP 200 + 业务码）。
 * <p>未标注 {@link RequirePerm} 的接口一律放行，保持存量接口行为不变。
 */
@Component
@RequiredArgsConstructor
public class PermInterceptor implements HandlerInterceptor {

    private final UserPermService userPermService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePerm require = handlerMethod.getMethodAnnotation(RequirePerm.class);
        if (require == null) {
            require = handlerMethod.getBeanType().getAnnotation(RequirePerm.class);
        }
        if (require == null) {
            return true;
        }
        LoginUser user = UserContext.get();
        if (user == null || user.isCustomer()) {
            return deny(response);
        }
        List<String> codes = new ArrayList<>();
        for (String c : require.value()) {
            if (c != null && !c.isEmpty()) {
                codes.add(c);
            }
        }
        String pathVar = require.pathVar();
        if (!pathVar.isEmpty() && !require.prefix().isEmpty()) {
            Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (attr instanceof Map<?, ?> vars) {
                Object v = vars.get(pathVar);
                if (v != null) {
                    codes.add(require.prefix() + String.valueOf(v).toUpperCase());
                }
            }
        }
        if (userPermService.hasAny(user.getUserId(), codes)) {
            return true;
        }
        return deny(response);
    }

    private boolean deny(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(ErrorCode.NO_PERMISSION, "当前岗位无权访问该功能")));
        response.getWriter().flush();
        return false;
    }
}
