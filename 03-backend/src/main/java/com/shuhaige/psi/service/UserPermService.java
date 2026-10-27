package com.shuhaige.psi.service;

import com.shuhaige.psi.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 岗位权限判定。
 * <p>匹配规则刻意与前端 store/user.js 的 hasPerm 保持一致（含「父级码隐含子码」），
 * 否则会出现「菜单能看见、接口却 403」的不一致。
 * <p>注意：后端不做「空权限数组全放行」的兜底 —— 那只是前端为菜单展示留的兼容，
 * 权限判定作为安全边界必须默认拒绝。
 */
@Service
@RequiredArgsConstructor
public class UserPermService {

    private final SysUserMapper userMapper;

    public boolean hasAny(Long userId, Collection<String> codes) {
        if (userId == null || codes == null || codes.isEmpty()) {
            return false;
        }
        List<String> roles = userMapper.selectRoleCodes(userId);
        if (roles != null) {
            for (String r : roles) {
                if ("ADMIN".equalsIgnoreCase(r)) {
                    return true;
                }
            }
        }
        List<String> perms = userMapper.selectPermCodes(userId);
        if (perms == null || perms.isEmpty()) {
            return false;
        }
        if (perms.contains("*")) {
            return true;
        }
        for (String code : codes) {
            if (code == null || code.isEmpty()) {
                continue;
            }
            int idx = code.indexOf(':');
            String top = idx > 0 ? code.substring(0, idx) : code;
            for (String p : perms) {
                if (p == null || p.isEmpty()) {
                    continue;
                }
                // ① 精确命中 ② 父级通配 ③ 父级码隐含子码（与前端规则一致）
                if (p.equals(code) || p.equals(top + ":*") || code.startsWith(p + ":")) {
                    return true;
                }
            }
        }
        return false;
    }
}
