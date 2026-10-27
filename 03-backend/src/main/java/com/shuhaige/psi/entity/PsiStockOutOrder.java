package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_stock_out_order")
public class PsiStockOutOrder implements LockAware {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long customerId;
    private Long warehouseId;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal payAmount;
    private String paymentType;
    private String status;
    /** 是否已确认锁定 1是 0否 */
    private Integer isLocked;
    private Long confirmUserId;
    private String confirmUserName;
    private LocalDateTime confirmTime;
    /** 是否已归档 1是 0否 */
    private Integer isArchived;
    private LocalDateTime archiveTime;
    private Long archiveUserId;
    private String archiveUserName;
    private String source;
    private String remark;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
    private LocalDateTime outTime;
    private LocalDateTime updateTime;

    @Override
    public String bizNo() {
        return orderNo;
    }
}
