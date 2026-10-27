package com.shuhaige.psi.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解，由 {@link PermInterceptor} 校验。
 * 用法一：直接列出权限码 {@code @RequirePerm("order:confirm")}（任一命中即放行）。
 * 用法二：按路径变量动态拼接 {@code @RequirePerm(prefix = "report:", pathVar = "code")}
 *         —— 取路径变量值并转大写后与 prefix 拼接，避免 /report/r01 绕过校验。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {

    /** 权限码，任一命中即放行 */
    String[] value() default {};

    /** 动态权限码前缀 */
    String prefix() default "";

    /** 路径变量名 */
    String pathVar() default "";
}
