package com.shuhaige.psi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.dto.LoginDTO;
import com.shuhaige.psi.entity.SysUser;
import com.shuhaige.psi.mapper.SysUserMapper;
import com.shuhaige.psi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(LoginDTO dto) {
        String account = dto.getUsername();
        // 先按工号匹配，未命中再按登录账号匹配（LoginDTO 保持不变，工号由客户端填入 username 字段，二者登录兼容）
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmployeeNo, account));
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, account));
        }
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.LOGIN_FAIL, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BizException(ErrorCode.USER_DISABLED, "账号被停用");
        }
        SysUser upd = new SysUser();
        upd.setId(user.getId());
        upd.setLastLogin(LocalDateTime.now());
        userMapper.updateById(upd);

        String token = jwtUtil.generate(user.getId(), user.getUsername());
        List<String> roles = userMapper.selectRoleCodes(user.getId());
        List<String> perms = userMapper.selectPermCodes(user.getId());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("employeeNo", user.getEmployeeNo());
        userInfo.put("realName", user.getRealName());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("roles", roles);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", userInfo);
        data.put("permissions", perms);
        return data;
    }

    public Map<String, Object> me(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("employeeNo", user.getEmployeeNo());
        data.put("realName", user.getRealName());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("avatar", user.getAvatar());
        data.put("roles", userMapper.selectRoleCodes(user.getId()));
        data.put("permissions", userMapper.selectPermCodes(user.getId()));
        return data;
    }
}