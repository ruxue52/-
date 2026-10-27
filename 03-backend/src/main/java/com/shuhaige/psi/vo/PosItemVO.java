package com.shuhaige.psi.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * POS 检索结果：图书 + 当前仓库存
 */
@Data
public class PosItemVO {
    private Long bookId;
    private String isbn;
    private String bookName;
    private String author;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String unit;
    private String shelfNo;
    private BigDecimal quantity;
    private String warehouseName;
}