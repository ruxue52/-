package com.shuhaige.psi.dto;

import lombok.Data;

import java.util.List;

/** 角色权限保存入参 */
@Data
public class RolePermDTO {
    /** 勾选的权限 ID 集合（父节点半选也应一并提交，否则菜单父级码丢失） */
    private List<Long> permissionIds;
}
