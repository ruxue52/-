package com.shuhaige.psi.dto;

import lombok.Data;

/** 修订审批入参（批准/驳回通用） */
@Data
public class RevisionAuditDTO {
    /** 审批意见 */
    private String remark;
}
