package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.PasswordDTO;
import com.shuhaige.psi.dto.RolePermDTO;
import com.shuhaige.psi.dto.StatusDTO;
import com.shuhaige.psi.dto.UserDTO;
import com.shuhaige.psi.entity.SysDictItem;
import com.shuhaige.psi.entity.SysOperationLog;
import com.shuhaige.psi.entity.SysPermission;
import com.shuhaige.psi.entity.SysRole;
import com.shuhaige.psi.entity.SysUser;
import com.shuhaige.psi.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class SystemController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysOperationLogMapper logMapper;
    private final SysDictItemMapper dictItemMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // ---------------- 用户 ----------------

    @GetMapping("/users")
    public Result<PageResult<Map<String, Object>>> users(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Integer status) {
        IPage<Map<String, Object>> p = userMapper.pageUsers(new Page<>(page, size), keyword, status);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @PostMapping("/users")
    public Result<Long> createUser(@RequestBody UserDTO dto) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "用户名已存在");
        }
        String employeeNo = normalizeEmployeeNo(dto.getEmployeeNo());
        if (employeeNo != null) {
            Long empExists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
            if (empExists != null && empExists > 0) {
                throw new BizException(ErrorCode.DUPLICATE, "工号已存在");
            }
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setEmployeeNo(employeeNo);
        user.setPassword(passwordEncoder.encode(dto.getPassword() == null ? "123456" : dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        saveRoles(user.getId(), dto.getRoleIds());
        return Result.ok(user.getId());
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        String employeeNo = normalizeEmployeeNo(dto.getEmployeeNo());
        if (employeeNo != null) {
            Long empExists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmployeeNo, employeeNo)
                    .ne(SysUser::getId, id));
            if (empExists != null && empExists > 0) {
                throw new BizException(ErrorCode.DUPLICATE, "工号已存在");
            }
        }
        user.setEmployeeNo(employeeNo);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(id);
            saveRoles(id, dto.getRoleIds());
        }
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
        return Result.ok();
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setStatus(dto.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.ok();
    }

    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setPassword(passwordEncoder.encode(dto.getPassword() == null ? "123456" : dto.getPassword()));
        upd.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(upd);
        return Result.ok();
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            userRoleMapper.insert(userId, roleId);
        }
    }

    /** 空串统一按 null 处理，避免多个用户共用空工号触发唯一键冲突 */
    private String normalizeEmployeeNo(String employeeNo) {
        if (employeeNo == null) {
            return null;
        }
        String trimmed = employeeNo.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ---------------- 角色 / 权限 ----------------

    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> roles() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysRole r : roles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("roleCode", r.getRoleCode());
            m.put("roleName", r.getRoleName());
            m.put("description", r.getDescription());
            m.put("status", r.getStatus());
            m.put("permissionIds", rolePermissionMapper.selectPermissionIds(r.getId()));
            list.add(m);
        }
        return Result.ok(list);
    }

    /** 保存角色权限：先清空该角色的授权行，再批量写入勾选（含父节点半选）的权限 */
    @PutMapping("/roles/{id}/permissions")
    @Transactional
    public Result<Void> updateRolePermissions(@PathVariable Long id, @RequestBody RolePermDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BizException.notFound("角色不存在");
        }
        rolePermissionMapper.deleteByRoleId(id);
        List<Long> ids = dto.getPermissionIds();
        if (ids != null && !ids.isEmpty()) {
            rolePermissionMapper.batchInsert(id, ids.stream().distinct().toList());
        }
        return Result.ok();
    }

    @GetMapping("/permissions/tree")
    public Result<List<Map<String, Object>>> permissionTree() {
        List<SysPermission> all = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSortNo).orderByAsc(SysPermission::getId));
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (SysPermission p : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", p.getId());
            node.put("permCode", p.getPermCode());
            node.put("permName", p.getPermName());
            node.put("permType", p.getPermType());
            node.put("parentId", p.getParentId());
            node.put("path", p.getPath());
            node.put("icon", p.getIcon());
            node.put("sortNo", p.getSortNo());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(p.getId(), node);
        }
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysPermission p : all) {
            Map<String, Object> node = nodeMap.get(p.getId());
            Map<String, Object> parent = nodeMap.get(p.getParentId());
            if (p.getParentId() != null && p.getParentId() != 0 && parent != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(node);
            } else {
                tree.add(node);
            }
        }
        return Result.ok(tree);
    }

    // ---------------- 操作日志 ----------------

    @GetMapping("/logs")
    public Result<PageResult<SysOperationLog>> logs(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String module,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<SysOperationLog> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(SysOperationLog::getUsername, keyword).or().like(SysOperationLog::getOperation, keyword));
        }
        if (module != null && !module.isEmpty()) {
            qw.eq(SysOperationLog::getModule, module);
        }
        if (startDate != null && !startDate.isEmpty()) {
            qw.ge(SysOperationLog::getCreateTime, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            qw.le(SysOperationLog::getCreateTime, endDate + " 23:59:59");
        }
        qw.orderByDesc(SysOperationLog::getId);
        IPage<SysOperationLog> p = logMapper.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    // ---------------- 字典 ----------------

    @GetMapping("/dicts/{dictCode}/items")
    public Result<List<SysDictItem>> dictItems(@PathVariable String dictCode) {
        return Result.ok(dictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, dictCode)
                .orderByAsc(SysDictItem::getSortNo)));
    }
}