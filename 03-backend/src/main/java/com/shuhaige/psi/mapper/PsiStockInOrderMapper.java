package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuhaige.psi.entity.PsiStockInOrder;
import com.shuhaige.psi.vo.PurchaseOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PsiStockInOrderMapper extends BaseMapper<PsiStockInOrder> {

    @Select("<script>SELECT o.*, s.supplier_name, w.warehouse_name FROM psi_stock_in_order o " +
            "LEFT JOIN base_supplier s ON s.id = o.supplier_id " +
            "LEFT JOIN base_warehouse w ON w.id = o.warehouse_id <where>o.is_archived = 0" +
            "<if test=\"status != null and status != ''\"> AND o.status = #{status}</if>" +
            "<if test=\"supplierId != null\"> AND o.supplier_id = #{supplierId}</if>" +
            "<if test=\"warehouseId != null\"> AND o.warehouse_id = #{warehouseId}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND o.order_no LIKE CONCAT('%',#{keyword},'%')</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND o.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.create_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "</where> ORDER BY o.id DESC</script>")
    IPage<PurchaseOrderVO> pageOrders(IPage<PurchaseOrderVO> page,
                                      @Param("status") String status,
                                      @Param("supplierId") Long supplierId,
                                      @Param("warehouseId") Long warehouseId,
                                      @Param("keyword") String keyword,
                                      @Param("startDate") String startDate,
                                      @Param("endDate") String endDate);

    @Select("SELECT o.*, s.supplier_name, w.warehouse_name FROM psi_stock_in_order o " +
            "LEFT JOIN base_supplier s ON s.id = o.supplier_id " +
            "LEFT JOIN base_warehouse w ON w.id = o.warehouse_id WHERE o.id = #{id}")
    PurchaseOrderVO selectDetail(@Param("id") Long id);

    @Select("SELECT IFNULL(MAX(order_no), '') FROM psi_stock_in_order WHERE order_no LIKE CONCAT(#{prefix},'%')")
    String maxOrderNo(@Param("prefix") String prefix);
}