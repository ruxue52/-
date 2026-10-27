package com.shuhaige.psi.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseOrderDTO {
    private Long supplierId;
    private Long warehouseId;
    private String settleType;
    private BigDecimal discount;
    private BigDecimal paidAmount;
    private String remark;
    private String source;
    private List<Item> items;

    @Data
    public static class Item {
        private Long bookId;
        private BigDecimal quantity;
        private BigDecimal price;
        private String batchNo;
        private String remark;
    }
}