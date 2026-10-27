package com.shuhaige.psi.vo;

import com.shuhaige.psi.entity.BaseBook;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookVO extends BaseBook {
    private String publisherName;
    private String categoryName;
}