package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("psi_stock_transfer_item")
public class PsiStockTransferItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long bookId;
    private String bookName;
    private String isbn;
    private BigDecimal quantity;
}