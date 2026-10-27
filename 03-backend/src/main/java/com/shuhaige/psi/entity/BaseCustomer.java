package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("base_customer")
public class BaseCustomer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerCode;
    private String customerName;
    private String customerType;
    private String phone;
    private String address;
    private Long levelId;
    private Integer points;
    private BigDecimal balance;
    private Integer status;
    private LocalDateTime createTime;
}