package com.shuhaige.psi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计（22 项）与报表（15 张）SQL 集中定义
 */
@Mapper
public interface ReportMapper {

    // ==================== 8. 查询统计 ====================

    @Select("SELECT " +
            "(SELECT IFNULL(SUM(pay_amount),0) FROM psi_stock_out_order WHERE status='OUTSTOCK' AND DATE(out_time)=CURDATE()) AS todaySales, " +
            "(SELECT COUNT(*) FROM psi_stock_out_order WHERE status='OUTSTOCK' AND DATE(out_time)=CURDATE()) AS todayOrders, " +
            "(SELECT IFNULL(SUM(pay_amount),0) FROM psi_stock_out_order WHERE status='OUTSTOCK' AND DATE_FORMAT(out_time,'%Y-%m')=DATE_FORMAT(CURDATE(),'%Y-%m')) AS monthSales, " +
            "(SELECT IFNULL(SUM(quantity*avg_cost),0) FROM psi_stock) AS stockAmount, " +
            "(SELECT COUNT(*) FROM psi_stock WHERE quantity < safety_stock) AS warningCount, " +
            "(SELECT IFNULL(SUM(pay_amount),0) FROM psi_stock_out_order WHERE status='OUTSTOCK') AS totalSales, " +
            "((SELECT COUNT(*) FROM psi_stock_in_order WHERE status IN ('DRAFT','SUBMITTED')) + (SELECT COUNT(*) FROM psi_stock_out_order WHERE status IN ('DRAFT','SUBMITTED'))) AS pendingOrders")
    Map<String, Object> overview();

    @Select("SELECT DATE_FORMAT(out_time,'%Y-%m-%d') AS date, IFNULL(SUM(pay_amount),0) AS amount, IFNULL(SUM(total_qty),0) AS qty " +
            "FROM psi_stock_out_order WHERE status='OUTSTOCK' AND out_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(out_time,'%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> salesTrend(@Param("days") int days);

    @Select("SELECT DATE_FORMAT(out_time,'%m') AS month, IFNULL(SUM(pay_amount),0) AS amount, IFNULL(SUM(total_qty),0) AS qty " +
            "FROM psi_stock_out_order WHERE status='OUTSTOCK' AND YEAR(out_time)=#{year} " +
            "GROUP BY DATE_FORMAT(out_time,'%m') ORDER BY month")
    List<Map<String, Object>> salesMonthly(@Param("year") int year);

    @Select("<script>SELECT b.book_name AS name, b.isbn AS isbn, IFNULL(SUM(i.quantity),0) AS qty, IFNULL(SUM(i.amount),0) AS amount " +
            "FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id JOIN base_book b ON b.id=i.book_id " +
            "WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY i.book_id, b.book_name, b.isbn ORDER BY qty DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> salesTopBooks(@Param("limit") Integer limit, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT IFNULL(c.category_name,'未分类') AS name, IFNULL(SUM(i.amount),0) AS value, IFNULL(SUM(i.quantity),0) AS qty " +
            "FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id JOIN base_book b ON b.id=i.book_id " +
            "LEFT JOIN base_category c ON c.id=b.category_id WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY c.id, c.category_name ORDER BY value DESC</script>")
    List<Map<String, Object>> salesByCategory(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT IFNULL(p.publisher_name,'未知出版社') AS name, IFNULL(SUM(i.amount),0) AS value, IFNULL(SUM(i.quantity),0) AS qty " +
            "FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id JOIN base_book b ON b.id=i.book_id " +
            "LEFT JOIN base_publisher p ON p.id=b.publisher_id WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY p.id, p.publisher_name ORDER BY value DESC</script>")
    List<Map<String, Object>> salesByPublisher(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT w.warehouse_name AS name, IFNULL(SUM(o.pay_amount),0) AS value, IFNULL(SUM(o.total_qty),0) AS qty " +
            "FROM psi_stock_out_order o JOIN base_warehouse w ON w.id=o.warehouse_id WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY o.warehouse_id, w.warehouse_name ORDER BY value DESC</script>")
    List<Map<String, Object>> salesByWarehouse(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT o.payment_type AS name, COUNT(*) AS value, IFNULL(SUM(o.pay_amount),0) AS amount " +
            "FROM psi_stock_out_order o WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY o.payment_type ORDER BY amount DESC</script>")
    List<Map<String, Object>> salesByPayment(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT IFNULL(o.operator_name,'未知') AS name, COUNT(*) AS orders, IFNULL(SUM(o.pay_amount),0) AS value " +
            "FROM psi_stock_out_order o WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY o.operator_id, o.operator_name ORDER BY value DESC</script>")
    List<Map<String, Object>> salesByOperator(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT HOUR(o.out_time) AS hour, COUNT(*) AS orders, IFNULL(SUM(o.pay_amount),0) AS amount " +
            "FROM psi_stock_out_order o WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY HOUR(o.out_time) ORDER BY hour</script>")
    List<Map<String, Object>> salesByHour(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT DATE_FORMAT(in_time,'%Y-%m-%d') AS date, IFNULL(SUM(pay_amount),0) AS amount, IFNULL(SUM(total_qty),0) AS qty " +
            "FROM psi_stock_in_order WHERE status='INSTOCK' AND in_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(in_time,'%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> purchaseTrend(@Param("days") int days);

    @Select("<script>SELECT s.supplier_name AS name, COUNT(DISTINCT o.id) AS orders, IFNULL(SUM(o.total_qty),0) AS qty, IFNULL(SUM(o.pay_amount),0) AS value " +
            "FROM psi_stock_in_order o JOIN base_supplier s ON s.id=o.supplier_id WHERE o.status='INSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.in_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.in_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY o.supplier_id, s.supplier_name ORDER BY value DESC</script>")
    List<Map<String, Object>> purchaseBySupplier(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT b.book_name AS name, b.isbn AS isbn, IFNULL(SUM(i.quantity),0) AS qty, IFNULL(SUM(i.amount),0) AS amount " +
            "FROM psi_stock_in_item i JOIN psi_stock_in_order o ON o.id=i.order_id JOIN base_book b ON b.id=i.book_id " +
            "WHERE o.status='INSTOCK' GROUP BY i.book_id, b.book_name, b.isbn ORDER BY qty DESC LIMIT #{limit}")
    List<Map<String, Object>> purchaseTopBooks(@Param("limit") Integer limit);

    @Select("SELECT IFNULL(c.category_name,'未分类') AS name, IFNULL(SUM(s.quantity*s.avg_cost),0) AS value, " +
            "COUNT(DISTINCT s.book_id) AS skuCount FROM psi_stock s JOIN base_book b ON b.id=s.book_id " +
            "LEFT JOIN base_category c ON c.id=b.category_id GROUP BY c.id, c.category_name ORDER BY value DESC")
    List<Map<String, Object>> stockValueByCategory();

    @Select("<script>SELECT " +
            "(SELECT IFNULL(SUM(i.quantity*i.cost_price),0) FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id " +
            "WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>) AS saleCost, " +
            "(SELECT IFNULL(SUM(quantity*avg_cost),0) FROM psi_stock) AS avgStockAmount, " +
            "(SELECT IFNULL(SUM(quantity),0) FROM psi_stock) AS avgStockQty</script>")
    Map<String, Object> stockTurnover(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT CASE WHEN DATEDIFF(CURDATE(), last_in_time) <= 30 THEN '0-30天' " +
            "WHEN DATEDIFF(CURDATE(), last_in_time) <= 60 THEN '31-60天' " +
            "WHEN DATEDIFF(CURDATE(), last_in_time) <= 90 THEN '61-90天' ELSE '90天以上' END AS `range`, " +
            "COUNT(*) AS skuCount, IFNULL(SUM(quantity*avg_cost),0) AS amount FROM psi_stock GROUP BY `range`")
    List<Map<String, Object>> stockAge();

    @Select("<script>SELECT b.book_name AS name, b.isbn AS isbn, IFNULL(SUM(i.quantity),0) AS qty, " +
            "IFNULL(SUM(i.amount),0) AS amount, IFNULL(SUM(i.cost_price*i.quantity),0) AS cost, " +
            "IFNULL(SUM((i.price-i.cost_price)*i.quantity),0) AS profit " +
            "FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id JOIN base_book b ON b.id=i.book_id " +
            "WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY i.book_id, b.book_name, b.isbn ORDER BY profit DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> profitByBook(@Param("limit") Integer limit, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT DATE_FORMAT(o.out_time,'%m') AS month, IFNULL(SUM((i.price-i.cost_price)*i.quantity),0) AS profit, " +
            "IFNULL(SUM(i.amount),0) AS amount FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id " +
            "WHERE o.status='OUTSTOCK' AND YEAR(o.out_time)=#{year} GROUP BY DATE_FORMAT(o.out_time,'%m') ORDER BY month")
    List<Map<String, Object>> profitMonthly(@Param("year") int year);

    @Select("SELECT c.customer_name AS name, c.customer_type AS customerType, COUNT(*) AS orders, " +
            "IFNULL(SUM(o.pay_amount),0) AS value FROM psi_stock_out_order o JOIN base_customer c ON c.id=o.customer_id " +
            "WHERE o.status='OUTSTOCK' GROUP BY o.customer_id, c.customer_name, c.customer_type ORDER BY value DESC LIMIT #{limit}")
    List<Map<String, Object>> customerConsumeRank(@Param("limit") Integer limit);

    @Select("SELECT COUNT(DISTINCT o.customer_id) AS activeMembers, " +
            "(SELECT COUNT(*) FROM (SELECT customer_id FROM psi_stock_out_order WHERE status='OUTSTOCK' AND customer_id IS NOT NULL " +
            "GROUP BY customer_id HAVING COUNT(*) > 1) t) AS repurchaseCustomers " +
            "FROM psi_stock_out_order o WHERE o.status='OUTSTOCK' AND o.customer_id IS NOT NULL " +
            "AND o.out_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
    Map<String, Object> customerActive(@Param("days") int days);

    @Select("SELECT IFNULL(l.level_name,'未分级') AS name, COUNT(c.id) AS value, IFNULL(SUM(c.points),0) AS points " +
            "FROM base_customer c LEFT JOIN base_member_level l ON l.id=c.level_id GROUP BY l.id, l.level_name ORDER BY l.id")
    List<Map<String, Object>> memberLevelDistribution();

    @Select("<script>SELECT o.check_no AS checkNo, o.warehouse_id AS warehouseId, w.warehouse_name AS warehouseName, " +
            "o.check_type AS checkType, o.status AS status, o.total_items AS totalItems, o.diff_items AS diffItems, " +
            "o.profit_qty AS profitQty, o.loss_qty AS lossQty, o.create_time AS createTime " +
            "FROM psi_stock_check_order o LEFT JOIN base_warehouse w ON w.id=o.warehouse_id WHERE 1=1 " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.create_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "ORDER BY o.id DESC</script>")
    List<Map<String, Object>> checkDiffSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // ==================== 9. 统计报表 R01 ~ R15 ====================

    @Select("SELECT o.order_no AS orderNo, DATE_FORMAT(o.out_time,'%Y-%m-%d %H:%i:%s') AS outTime, IFNULL(c.customer_name,'散客') AS customerName, " +
            "o.total_qty AS totalQty, o.total_amount AS totalAmount, o.pay_amount AS payAmount, o.payment_type AS paymentType, " +
            "o.operator_name AS operatorName FROM psi_stock_out_order o LEFT JOIN base_customer c ON c.id=o.customer_id " +
            "WHERE o.status='OUTSTOCK' AND DATE(o.out_time)=#{date} ORDER BY o.id")
    List<Map<String, Object>> r01(@Param("date") String date);

    @Select("SELECT DATE_FORMAT(out_time,'%Y-%m-%d') AS date, COUNT(*) AS orderCount, IFNULL(SUM(total_qty),0) AS totalQty, " +
            "IFNULL(SUM(total_amount),0) AS totalAmount, IFNULL(SUM(pay_amount),0) AS payAmount " +
            "FROM psi_stock_out_order WHERE status='OUTSTOCK' AND DATE_FORMAT(out_time,'%Y-%m')=#{month} " +
            "GROUP BY DATE_FORMAT(out_time,'%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> r02(@Param("month") String month);

    @Select("<script>SELECT b.isbn AS isbn, b.book_name AS bookName, b.author AS author, IFNULL(SUM(i.quantity),0) AS qty, " +
            "IFNULL(SUM(i.amount),0) AS amount FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id " +
            "JOIN base_book b ON b.id=i.book_id WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY i.book_id, b.isbn, b.book_name, b.author ORDER BY qty DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> r03(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") Integer limit);

    @Select("<script>SELECT IFNULL(c.category_name,'未分类') AS categoryName, COUNT(DISTINCT i.book_id) AS skuCount, " +
            "IFNULL(SUM(i.quantity),0) AS qty, IFNULL(SUM(i.amount),0) AS amount FROM psi_stock_out_item i " +
            "JOIN psi_stock_out_order o ON o.id=i.order_id JOIN base_book b ON b.id=i.book_id " +
            "LEFT JOIN base_category c ON c.id=b.category_id WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY c.id, c.category_name ORDER BY amount DESC</script>")
    List<Map<String, Object>> r04(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT o.order_no AS orderNo, DATE_FORMAT(o.in_time,'%Y-%m-%d %H:%i:%s') AS inTime, s.supplier_name AS supplierName, " +
            "i.isbn AS isbn, i.book_name AS bookName, i.quantity AS quantity, i.price AS price, i.amount AS amount " +
            "FROM psi_stock_in_item i JOIN psi_stock_in_order o ON o.id=i.order_id JOIN base_supplier s ON s.id=o.supplier_id " +
            "WHERE o.status='INSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.in_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.in_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "<if test=\"supplierId != null\"> AND o.supplier_id = #{supplierId}</if>" +
            "ORDER BY o.id DESC</script>")
    List<Map<String, Object>> r05(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("supplierId") Long supplierId);

    @Select("<script>SELECT s.supplier_name AS supplierName, s.contact AS contact, s.phone AS phone, COUNT(DISTINCT o.id) AS orderCount, " +
            "IFNULL(SUM(o.total_qty),0) AS totalQty, IFNULL(SUM(o.total_amount),0) AS totalAmount, IFNULL(SUM(o.pay_amount),0) AS payAmount " +
            "FROM psi_stock_in_order o JOIN base_supplier s ON s.id=o.supplier_id WHERE o.status='INSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.in_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.in_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY o.supplier_id, s.supplier_name, s.contact, s.phone ORDER BY payAmount DESC</script>")
    List<Map<String, Object>> r06(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT b.isbn AS isbn, b.book_name AS bookName, w.warehouse_name AS warehouseName, c.category_name AS categoryName, " +
            "s.quantity AS quantity, s.safety_stock AS safetyStock, s.avg_cost AS avgCost, ROUND(s.quantity*s.avg_cost,2) AS stockAmount " +
            "FROM psi_stock s JOIN base_book b ON b.id=s.book_id JOIN base_warehouse w ON w.id=s.warehouse_id " +
            "LEFT JOIN base_category c ON c.id=b.category_id WHERE 1=1 " +
            "<if test=\"warehouseId != null\"> AND s.warehouse_id = #{warehouseId}</if>" +
            "<if test=\"categoryId != null\"> AND b.category_id = #{categoryId}</if>" +
            "ORDER BY w.id, b.id</script>")
    List<Map<String, Object>> r07(@Param("warehouseId") Long warehouseId, @Param("categoryId") Long categoryId);

    @Select("<script>SELECT b.isbn AS isbn, b.book_name AS bookName, w.warehouse_name AS warehouseName, " +
            "s.quantity AS quantity, s.safety_stock AS safetyStock, (s.safety_stock - s.quantity) AS shortageQty " +
            "FROM psi_stock s JOIN base_book b ON b.id=s.book_id JOIN base_warehouse w ON w.id=s.warehouse_id " +
            "WHERE s.quantity &lt; s.safety_stock " +
            "<if test=\"warehouseId != null\"> AND s.warehouse_id = #{warehouseId}</if>" +
            "ORDER BY shortageQty DESC</script>")
    List<Map<String, Object>> r08(@Param("warehouseId") Long warehouseId);

    @Select("<script>SELECT b.isbn AS isbn, b.book_name AS bookName, w.warehouse_name AS warehouseName, s.quantity AS quantity, " +
            "DATE_FORMAT(s.last_in_time,'%Y-%m-%d') AS lastInTime, IFNULL(DATEDIFF(CURDATE(), s.last_in_time),0) AS ageDays, " +
            "ROUND(s.quantity*s.avg_cost,2) AS stockAmount FROM psi_stock s JOIN base_book b ON b.id=s.book_id " +
            "JOIN base_warehouse w ON w.id=s.warehouse_id WHERE 1=1 " +
            "<if test=\"warehouseId != null\"> AND s.warehouse_id = #{warehouseId}</if>" +
            "ORDER BY ageDays DESC</script>")
    List<Map<String, Object>> r09(@Param("warehouseId") Long warehouseId);

    @Select("<script>SELECT o.check_no AS checkNo, w.warehouse_name AS warehouseName, i.isbn AS isbn, i.book_name AS bookName, " +
            "i.book_qty AS bookQty, i.real_qty AS realQty, i.diff_qty AS diffQty, i.diff_amount AS diffAmount " +
            "FROM psi_stock_check_item i JOIN psi_stock_check_order o ON o.id=i.order_id LEFT JOIN base_warehouse w ON w.id=o.warehouse_id " +
            "WHERE i.diff_qty &lt;&gt; 0 " +
            "<if test=\"checkNo != null and checkNo != ''\"> AND o.check_no = #{checkNo}</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND o.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.create_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "ORDER BY o.id DESC, i.id</script>")
    List<Map<String, Object>> r10(@Param("checkNo") String checkNo, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT b.isbn AS isbn, b.book_name AS bookName, IFNULL(SUM(i.quantity),0) AS qty, " +
            "IFNULL(SUM(i.amount),0) AS amount, IFNULL(SUM(i.cost_price*i.quantity),0) AS costAmount, " +
            "IFNULL(SUM((i.price-i.cost_price)*i.quantity),0) AS profit, " +
            "ROUND(IFNULL(SUM((i.price-i.cost_price)*i.quantity),0)/NULLIF(SUM(i.amount),0)*100,2) AS profitRate " +
            "FROM psi_stock_out_item i JOIN psi_stock_out_order o ON o.id=i.order_id JOIN base_book b ON b.id=i.book_id " +
            "WHERE o.status='OUTSTOCK' " +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "GROUP BY i.book_id, b.isbn, b.book_name ORDER BY profit DESC</script>")
    List<Map<String, Object>> r11(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT o.order_no AS orderNo, DATE_FORMAT(o.out_time,'%Y-%m-%d %H:%i:%s') AS outTime, " +
            "IFNULL(c.customer_name,'散客') AS customerName, o.total_qty AS totalQty, o.total_amount AS totalAmount, " +
            "o.discount AS discount, o.pay_amount AS payAmount, o.payment_type AS paymentType " +
            "FROM psi_stock_out_order o LEFT JOIN base_customer c ON c.id=o.customer_id WHERE o.status='OUTSTOCK' " +
            "<if test=\"customerId != null\"> AND o.customer_id = #{customerId}</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND o.out_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND o.out_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "ORDER BY o.id DESC</script>")
    List<Map<String, Object>> r12(@Param("customerId") Long customerId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT c.customer_code AS customerCode, c.customer_name AS customerName, l.level_name AS levelName, " +
            "c.points AS points, c.balance AS balance, c.phone AS phone FROM base_customer c " +
            "LEFT JOIN base_member_level l ON l.id=c.level_id WHERE c.customer_type='MEMBER' " +
            "<if test=\"levelId != null\"> AND c.level_id = #{levelId}</if>" +
            "ORDER BY c.points DESC</script>")
    List<Map<String, Object>> r13(@Param("levelId") Long levelId);

    @Select("<script>SELECT f.biz_type AS bizType, f.biz_no AS bizNo, b.isbn AS isbn, b.book_name AS bookName, " +
            "w.warehouse_name AS warehouseName, f.direction AS direction, f.quantity AS quantity, f.before_qty AS beforeQty, " +
            "f.after_qty AS afterQty, f.unit_cost AS unitCost, DATE_FORMAT(f.create_time,'%Y-%m-%d %H:%i:%s') AS createTime " +
            "FROM psi_stock_flow f LEFT JOIN base_book b ON b.id=f.book_id LEFT JOIN base_warehouse w ON w.id=f.warehouse_id " +
            "WHERE 1=1 " +
            "<if test=\"bookId != null\"> AND f.book_id = #{bookId}</if>" +
            "<if test=\"warehouseId != null\"> AND f.warehouse_id = #{warehouseId}</if>" +
            "<if test=\"startDate != null and startDate != ''\"> AND f.create_time &gt;= #{startDate}</if>" +
            "<if test=\"endDate != null and endDate != ''\"> AND f.create_time &lt;= CONCAT(#{endDate},' 23:59:59')</if>" +
            "ORDER BY f.id DESC LIMIT 2000</script>")
    List<Map<String, Object>> r14(@Param("bookId") Long bookId, @Param("warehouseId") Long warehouseId,
                                  @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT b.isbn AS isbn, b.book_name AS bookName, w.warehouse_name AS warehouseName, " +
            "IFNULL((SELECT SUM(f.quantity) FROM psi_stock_flow f WHERE f.book_id=s.book_id AND f.warehouse_id=s.warehouse_id " +
            "AND f.direction=1 AND f.biz_type='IN' AND DATE_FORMAT(f.create_time,'%Y-%m')=#{month}),0) AS inQty, " +
            "IFNULL((SELECT SUM(f.quantity) FROM psi_stock_flow f WHERE f.book_id=s.book_id AND f.warehouse_id=s.warehouse_id " +
            "AND f.direction=-1 AND DATE_FORMAT(f.create_time,'%Y-%m')=#{month}),0) AS outQty, " +
            "s.quantity AS endQty, ROUND(s.quantity*s.avg_cost,2) AS stockAmount " +
            "FROM psi_stock s JOIN base_book b ON b.id=s.book_id JOIN base_warehouse w ON w.id=s.warehouse_id WHERE 1=1 " +
            "<if test=\"warehouseId != null\"> AND s.warehouse_id = #{warehouseId}</if>" +
            "ORDER BY w.id, b.id</script>")
    List<Map<String, Object>> r15(@Param("month") String month, @Param("warehouseId") Long warehouseId);
}