package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuhaige.psi.entity.PsiStockCheckOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PsiStockCheckOrderMapper extends BaseMapper<PsiStockCheckOrder> {

    @Select("SELECT IFNULL(MAX(check_no), '') FROM psi_stock_check_order WHERE check_no LIKE CONCAT(#{prefix},'%')")
    String maxCheckNo(@Param("prefix") String prefix);
}