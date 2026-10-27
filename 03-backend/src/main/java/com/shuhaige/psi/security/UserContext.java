package com.shuhaige.psi.security;

/**
 * 基于 ThreadLocal 的当前登录用户上下文
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String getUsername() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUsername();
    }

    /** STAFF / CUSTOMER，未登录返回 null */
    public static String getUserType() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserType();
    }

    public static void clear() {
        HOLDER.remove();
    }
}