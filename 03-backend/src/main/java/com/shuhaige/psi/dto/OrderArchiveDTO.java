package com.shuhaige.psi.dto;

import lombok.Data;

/** 批量归档入参 */
@Data
public class OrderArchiveDTO {
    /** 单据类型 IN/OUT/CHECK/TRANSFER/RETURN */
    private String orderType;
    /** 保留天数：仅归档创建时间早于「今天 - keepDays」的终态单据，默认 180 */
    private Integer keepDays;
}
