package com.shuhaige.psi.service;

import com.shuhaige.psi.entity.SysUser;
import com.shuhaige.psi.mapper.SysUserMapper;
import com.shuhaige.psi.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 解析当前操作人（来自登录上下文）
 */
@Component
@RequiredArgsConstructor
public class CurrentOperator {

    private final SysUserMapper userMapper;

    public Long id() {
        return UserContext.getUserId();
    }

    public String name() {
        Long uid = UserContext.getUserId();
        if (uid == null) {
            return "系统";
        }
        SysUser u = userMapper.selectById(uid);
        if (u == null) {
            return UserContext.getUsername();
        }
        return u.getRealName() == null || u.getRealName().isEmpty() ? u.getUsername() : u.getRealName();
    }
}