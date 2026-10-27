package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("psi_stock_out_item")
public class PsiStockOutItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long bookId;
    private String bookName;
    private String isbn;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal costPrice;
    private String remark;
}