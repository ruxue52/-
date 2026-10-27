package com.shuhaige.psi.vo;

import com.shuhaige.psi.entity.BaseCustomer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerVO extends BaseCustomer {
    private String levelName;
    private BigDecimal discount;
}