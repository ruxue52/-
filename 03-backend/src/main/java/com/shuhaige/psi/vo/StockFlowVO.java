package com.shuhaige.psi.vo;

import com.shuhaige.psi.entity.PsiStockFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockFlowVO extends PsiStockFlow {
    private String bookName;
    private String isbn;
    private String warehouseName;
}