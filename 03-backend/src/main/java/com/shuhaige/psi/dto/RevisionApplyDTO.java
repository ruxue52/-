package com.shuhaige.psi.dto;

import lombok.Data;

/** 修订申请入参 */
@Data
public class RevisionApplyDTO {
    /** 修订原因，必填 */
    private String reason;
}
