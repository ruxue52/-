package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("psi_stock_in_log")
public class PsiStockInLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}