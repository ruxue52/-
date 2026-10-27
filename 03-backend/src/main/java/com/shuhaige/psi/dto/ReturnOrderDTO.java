package com.shuhaige.psi.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnOrderDTO {
    /** PURCHASE 采购退货 / SALES 销售退货 */
    private String returnType;
    private Long partnerId;
    private Long warehouseId;
    private String reason;
    private List<Item> items;

    @Data
    public static class Item {
        private Long bookId;
        private BigDecimal quantity;
        private BigDecimal price;
    }
}