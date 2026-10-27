package com.shuhaige.psi.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户（来自 JWT 载荷）
 * userType 用于区分身份：员工（管理端/员工端）与会员（读者端）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 员工：sys_user 账号 */
    public static final String TYPE_STAFF = "STAFF";
    /** 会员：base_customer 账号（读者端） */
    public static final String TYPE_CUSTOMER = "CUSTOMER";

    private Long userId;
    private String username;
    /** STAFF / CUSTOMER */
    private String userType;

    public boolean isCustomer() {
        return TYPE_CUSTOMER.equals(userType);
    }
}
