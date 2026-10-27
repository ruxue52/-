package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuhaige.psi.entity.PsiStock;
import com.shuhaige.psi.vo.StockVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PsiStockMapper extends BaseMapper<PsiStock> {

    @Select("<script>SELECT s.*, b.book_name, b.isbn, c.category_name, w.warehouse_name, " +
            "ROUND(s.quantity * s.avg_cost, 2) AS stock_amount " +
            "FROM psi_stock s JOIN base_book b ON b.id = s.book_id " +
            "LEFT JOIN base_category c ON c.id = b.category_id " +
            "JOIN base_warehouse w ON w.id = s.warehouse_id <where>" +
            "<if test=\"warehouseId != null\"> AND s.warehouse_id = #{warehouseId}</if>" +
            "<if test=\"categoryId != null\"> AND b.category_id = #{categoryId}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (b.book_name LIKE CONCAT('%',#{keyword},'%') OR b.isbn LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test=\"onlyLow\"> AND s.quantity &lt; s.safety_stock</if>" +
            "</where> ORDER BY s.id</script>")
    IPage<StockVO> pageStock(IPage<StockVO> page,
                             @Param("warehouseId") Long warehouseId,
                             @Param("categoryId") Long categoryId,
                             @Param("keyword") String keyword,
                             @Param("onlyLow") boolean onlyLow);

    @Select("SELECT s.*, b.book_name, b.isbn, c.category_name, w.warehouse_name, " +
            "ROUND(s.quantity * s.avg_cost, 2) AS stock_amount " +
            "FROM psi_stock s JOIN base_book b ON b.id = s.book_id " +
            "LEFT JOIN base_category c ON c.id = b.category_id " +
            "JOIN base_warehouse w ON w.id = s.warehouse_id " +
            "WHERE s.book_id = #{bookId} ORDER BY s.warehouse_id")
    List<StockVO> listByBook(@Param("bookId") Long bookId);

    @Select("SELECT s.*, b.book_name, b.isbn, c.category_name, w.warehouse_name, " +
            "ROUND(s.quantity * s.avg_cost, 2) AS stock_amount " +
            "FROM psi_stock s JOIN base_book b ON b.id = s.book_id " +
            "LEFT JOIN base_category c ON c.id = b.category_id " +
            "JOIN base_warehouse w ON w.id = s.warehouse_id " +
            "WHERE s.quantity < s.safety_stock ORDER BY (s.safety_stock - s.quantity) DESC")
    List<StockVO> listWarnings();

    @Select("SELECT COUNT(*) AS totalSku, IFNULL(SUM(s.quantity),0) AS totalQty, " +
            "IFNULL(SUM(s.quantity * s.avg_cost),0) AS totalAmount, " +
            "IFNULL(SUM(CASE WHEN s.quantity < s.safety_stock THEN 1 ELSE 0 END),0) AS warningCount " +
            "FROM psi_stock s")
    Map<String, Object> summary();

    @Select("SELECT b.id AS bookId, b.isbn, b.book_name AS bookName, b.author, b.price, " +
            "b.sale_price AS salePrice, b.unit, b.shelf_no AS shelfNo, " +
            "IFNULL(s.quantity,0) AS quantity, w.warehouse_name AS warehouseName " +
            "FROM base_book b " +
            "LEFT JOIN psi_stock s ON s.book_id = b.id AND (#{warehouseId} IS NULL OR s.warehouse_id = #{warehouseId}) " +
            "LEFT JOIN base_warehouse w ON w.id = s.warehouse_id " +
            "WHERE (b.isbn LIKE CONCAT('%',#{keyword},'%') OR b.book_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR b.author LIKE CONCAT('%',#{keyword},'%')) AND b.status = 1 LIMIT 20")
    List<Map<String, Object>> posSearch(@Param("keyword") String keyword, @Param("warehouseId") Long warehouseId);
}