package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("base_book")
public class BaseBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String isbn;
    private String bookName;
    private String author;
    private Long publisherId;
    private Long categoryId;
    private String publishDate;
    private BigDecimal price;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private String unit;
    private String shelfNo;
    private String coverUrl;
    private String summary;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}