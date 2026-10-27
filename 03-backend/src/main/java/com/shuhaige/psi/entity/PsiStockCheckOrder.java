package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_stock_check_order")
public class PsiStockCheckOrder implements LockAware {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String checkNo;
    private Long warehouseId;
    private String checkType;
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
    private Integer totalItems;
    private Integer diffItems;
    private BigDecimal profitQty;
    private BigDecimal lossQty;
    private String remark;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;

    @Override
    public String bizNo() {
        return checkNo;
    }
}
