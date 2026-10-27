package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuhaige.psi.entity.PsiStockOutOrder;
import com.shuhaige.psi.vo.SaleOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface PsiStockOutOrderMapper extends BaseMapper<PsiStockOutOrder> {

    @Select("<script>SELECT o.*, c.customer_name, w.warehouse_name FROM psi_stock_out_order o " +
            "LEFT JOIN base_customer c ON c.id = o.customer_id " +
            "LEFT JOIN base_warehouse w ON w.id = o.warehouse_id <where>o.is_archived = 0" +
            "<if test=\"status != null and status != ''\"> AND o.status = #{status}</if>" +
            "<if test=\"customerId != null\"> AND o.customer_id = #{customerId}</if>" +
            "<if test=\"warehouseId != null\"> AND o.warehouse_id = #{warehouseId}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND o.order_no LIKE CONCAT('%',#{keyword},'%')</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND o.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.create_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "</where> ORDER BY o.id DESC</script>")
    IPage<SaleOrderVO> pageOrders(IPage<SaleOrderVO> page,
                                  @Param("status") String status,
                                  @Param("customerId") Long customerId,
                                  @Param("warehouseId") Long warehouseId,
                                  @Param("keyword") String keyword,
                                  @Param("startDate") String startDate,
                                  @Param("endDate") String endDate);

    @Select("SELECT o.*, c.customer_name, w.warehouse_name FROM psi_stock_out_order o " +
            "LEFT JOIN base_customer c ON c.id = o.customer_id " +
            "LEFT JOIN base_warehouse w ON w.id = o.warehouse_id WHERE o.id = #{id}")
    SaleOrderVO selectDetail(@Param("id") Long id);

    @Select("SELECT IFNULL(MAX(order_no), '') FROM psi_stock_out_order WHERE order_no LIKE CONCAT(#{prefix},'%')")
    String maxOrderNo(@Param("prefix") String prefix);

    /** 读者端：会员消费汇总（笔数 + 累计消费金额） */
    @Select("SELECT COUNT(*) AS orderCount, IFNULL(SUM(pay_amount), 0) AS totalAmount " +
            "FROM psi_stock_out_order WHERE customer_id = #{customerId} AND status = 'OUTSTOCK'")
    Map<String, Object> sumByCustomer(@Param("customerId") Long customerId);
}