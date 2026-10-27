package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_stock")
public class PsiStock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private Long warehouseId;
    private BigDecimal quantity;
    private BigDecimal lockedQty;
    private BigDecimal safetyStock;
    private BigDecimal avgCost;
    private LocalDateTime lastInTime;
    private LocalDateTime lastOutTime;
    private LocalDateTime updateTime;
}