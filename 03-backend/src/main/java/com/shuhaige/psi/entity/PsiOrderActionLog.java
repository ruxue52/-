package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单据操作流水（5 类单据统一台账，取代原 psi_stock_in_log 的新增写入）
 */
@Data
@TableName("psi_order_action_log")
public class PsiOrderActionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderType;
    private Long orderId;
    private String orderNo;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private String extJson;
    private LocalDateTime createTime;
}
