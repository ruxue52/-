package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuhaige.psi.entity.PsiReturnOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PsiReturnOrderMapper extends BaseMapper<PsiReturnOrder> {

    @Select("SELECT IFNULL(MAX(return_no), '') FROM psi_return_order WHERE return_no LIKE CONCAT(#{prefix},'%')")
    String maxReturnNo(@Param("prefix") String prefix);
}