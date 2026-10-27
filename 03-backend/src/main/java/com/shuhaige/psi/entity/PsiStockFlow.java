package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_stock_flow")
public class PsiStockFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private String bizNo;
    private Long bookId;
    private Long warehouseId;
    private Integer direction;
    private BigDecimal quantity;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;
    private BigDecimal unitCost;
    private Long operatorId;
    private String operatorName;
    private String source;
    private LocalDateTime createTime;
}