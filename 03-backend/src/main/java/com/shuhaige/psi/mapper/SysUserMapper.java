package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuhaige.psi.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT r.role_code FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} ORDER BY r.id")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("SELECT r.role_name FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} ORDER BY r.id")
    List<String> selectRoleNames(@Param("userId") Long userId);

    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p " +
            "JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "JOIN sys_user_role ur ON ur.role_id = rp.role_id WHERE ur.user_id = #{userId}")
    List<String> selectPermCodes(@Param("userId") Long userId);

    // 注意：本方法返回 Map，MyBatis 不会把列名下划线转驼峰，故显式用 AS 别名输出前端使用的驼峰键名
    @Select("<script>SELECT u.id, u.username, u.employee_no AS employeeNo, u.real_name AS realName, u.phone, u.email, u.gender, u.status, " +
            "u.last_login AS lastLogin, u.create_time AS createTime, " +
            "(SELECT GROUP_CONCAT(r.role_name) FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = u.id) AS roleNames " +
            "FROM sys_user u <where>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (u.username LIKE CONCAT('%',#{keyword},'%') OR u.real_name LIKE CONCAT('%',#{keyword},'%') OR u.employee_no LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test=\"status != null\"> AND u.status = #{status}</if>" +
            "</where> ORDER BY u.id</script>")
    IPage<Map<String, Object>> pageUsers(IPage<Map<String, Object>> page,
                                         @Param("keyword") String keyword,
                                         @Param("status") Integer status);
}