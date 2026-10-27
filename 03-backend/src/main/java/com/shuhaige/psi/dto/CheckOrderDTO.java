package com.shuhaige.psi.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CheckOrderDTO {
    private Long warehouseId;
    private String checkType;
    private String remark;
    /** 需盘点的图书ID，空表示全盘 */
    private List<Long> bookIds;

    @Data
    public static class ItemUpdate {
        private Long itemId;
        private BigDecimal realQty;
    }

    @Data
    public static class ItemsDTO {
        private List<ItemUpdate> items;
    }
}