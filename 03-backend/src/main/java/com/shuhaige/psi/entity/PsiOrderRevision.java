package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单据修订申请（锁定后修订的唯一在途凭据）
 * status: PENDING 待审批 / APPROVED 已批准已解锁 / REJECTED 已驳回 / CANCELLED 已撤销 / CLOSED 修订完成已重新确认
 */
@Data
@TableName("psi_order_revision")
public class PsiOrderRevision {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderType;
    private Long orderId;
    private String orderNo;
    private String reason;
    private String status;
    /** 修改前快照（JSON 字符串），审批解锁瞬间采集 */
    private String beforeSnapshot;
    /** 修改后快照（JSON 字符串），重新确认瞬间采集 */
    private String afterSnapshot;
    private Long applyUserId;
    private String applyUserName;
    private LocalDateTime applyTime;
    private Long approveUserId;
    private String approveUserName;
    private LocalDateTime approveTime;
    private String approveRemark;
    private LocalDateTime closeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
