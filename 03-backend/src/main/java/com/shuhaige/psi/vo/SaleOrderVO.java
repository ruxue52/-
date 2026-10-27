package com.shuhaige.psi.vo;

import com.shuhaige.psi.entity.PsiStockOutItem;
import com.shuhaige.psi.entity.PsiStockOutOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SaleOrderVO extends PsiStockOutOrder {
    private String customerName;
    private String warehouseName;
    private List<PsiStockOutItem> items;
}