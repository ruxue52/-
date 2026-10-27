package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuhaige.psi.entity.PsiStockTransferOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PsiStockTransferOrderMapper extends BaseMapper<PsiStockTransferOrder> {

    @Select("SELECT IFNULL(MAX(transfer_no), '') FROM psi_stock_transfer_order WHERE transfer_no LIKE CONCAT(#{prefix},'%')")
    String maxTransferNo(@Param("prefix") String prefix);
}