package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuhaige.psi.entity.PsiStockFlow;
import com.shuhaige.psi.vo.StockFlowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PsiStockFlowMapper extends BaseMapper<PsiStockFlow> {

    @Select("<script>SELECT f.*, b.book_name, b.isbn, w.warehouse_name FROM psi_stock_flow f " +
            "LEFT JOIN base_book b ON b.id = f.book_id " +
            "LEFT JOIN base_warehouse w ON w.id = f.warehouse_id <where>" +
            "<if test=\"bookId != null\"> AND f.book_id = #{bookId}</if>" +
            "<if test=\"warehouseId != null\"> AND f.warehouse_id = #{warehouseId}</if>" +
            "<if test=\"bizType != null and bizType != ''\"> AND f.biz_type = #{bizType}</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND f.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND f.create_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "</where> ORDER BY f.id DESC</script>")
    IPage<StockFlowVO> pageFlows(IPage<StockFlowVO> page,
                                 @Param("bookId") Long bookId,
                                 @Param("warehouseId") Long warehouseId,
                                 @Param("bizType") String bizType,
                                 @Param("startDate") String startDate,
                                 @Param("endDate") String endDate);
}