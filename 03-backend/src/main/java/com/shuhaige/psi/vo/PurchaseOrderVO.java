package com.shuhaige.psi.vo;

import com.shuhaige.psi.entity.PsiStockInItem;
import com.shuhaige.psi.entity.PsiStockInOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderVO extends PsiStockInOrder {
    private String supplierName;
    private String warehouseName;
    private List<PsiStockInItem> items;
}