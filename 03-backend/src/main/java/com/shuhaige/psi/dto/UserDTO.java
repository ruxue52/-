package com.shuhaige.psi.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private String username;
    private String employeeNo;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private Integer status;
    private List<Long> roleIds;
}