package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("base_member_level")
public class BaseMemberLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String levelCode;
    private String levelName;
    private BigDecimal discount;
    private BigDecimal pointRate;
}