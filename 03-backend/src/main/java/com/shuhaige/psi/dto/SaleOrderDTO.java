package com.shuhaige.psi.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleOrderDTO {
    private Long customerId;
    private Long warehouseId;
    private String paymentType;
    private BigDecimal discount;
    private BigDecimal payAmount;
    private String remark;
    private String source;
    /** true 表示新建后直接出库 */
    private Boolean directOut;
    private List<Item> items;

    @Data
    public static class Item {
        private Long bookId;
        private BigDecimal quantity;
        private BigDecimal price;
        private String remark;
    }
}