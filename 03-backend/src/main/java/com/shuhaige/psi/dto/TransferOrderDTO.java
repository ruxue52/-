package com.shuhaige.psi.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TransferOrderDTO {
    private Long fromWarehouse;
    private Long toWarehouse;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long bookId;
        private BigDecimal quantity;
    }
}