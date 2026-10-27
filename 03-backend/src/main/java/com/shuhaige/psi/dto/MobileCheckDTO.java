package com.shuhaige.psi.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MobileCheckDTO {
    private Long checkId;
    private String code;
    private BigDecimal realQty;
}