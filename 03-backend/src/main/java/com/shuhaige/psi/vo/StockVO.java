package com.shuhaige.psi.vo;

import com.shuhaige.psi.entity.PsiStock;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockVO extends PsiStock {
    private String bookName;
    private String isbn;
    private String categoryName;
    private String warehouseName;
    /** 库存金额 = quantity * avgCost */
    private BigDecimal stockAmount;
}