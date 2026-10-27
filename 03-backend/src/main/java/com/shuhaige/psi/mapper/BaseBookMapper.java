package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuhaige.psi.entity.BaseBook;
import com.shuhaige.psi.vo.BookVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BaseBookMapper extends BaseMapper<BaseBook> {

    @Select("<script>SELECT b.*, p.publisher_name, c.category_name FROM base_book b " +
            "LEFT JOIN base_publisher p ON p.id = b.publisher_id " +
            "LEFT JOIN base_category c ON c.id = b.category_id <where>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (b.book_name LIKE CONCAT('%',#{keyword},'%') OR b.isbn LIKE CONCAT('%',#{keyword},'%') OR b.author LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test=\"categoryId != null\"> AND b.category_id = #{categoryId}</if>" +
            "<if test=\"publisherId != null\"> AND b.publisher_id = #{publisherId}</if>" +
            "<if test=\"status != null\"> AND b.status = #{status}</if>" +
            "</where> ORDER BY b.id</script>")
    IPage<BookVO> pageBooks(IPage<BookVO> page,
                            @Param("keyword") String keyword,
                            @Param("categoryId") Long categoryId,
                            @Param("publisherId") Long publisherId,
                            @Param("status") Integer status);

    /** 读者端：仅在售图书分页（sort=newest 按最新上架倒序，默认按 id 升序） */
    @Select("<script>SELECT b.*, p.publisher_name, c.category_name FROM base_book b " +
            "LEFT JOIN base_publisher p ON p.id = b.publisher_id " +
            "LEFT JOIN base_category c ON c.id = b.category_id " +
            "WHERE b.status = 1 " +
            "<if test=\"keyword != null and keyword != ''\"> AND (b.book_name LIKE CONCAT('%',#{keyword},'%') OR b.isbn LIKE CONCAT('%',#{keyword},'%') OR b.author LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test=\"categoryIds != null and categoryIds.size() > 0\"> AND b.category_id IN " +
            "<foreach collection=\"categoryIds\" item=\"cid\" open=\"(\" separator=\",\" close=\")\">#{cid}</foreach></if>" +
            "<choose><when test=\"sort == 'newest'\"> ORDER BY b.id DESC</when><otherwise> ORDER BY b.id ASC</otherwise></choose>" +
            "</script>")
    IPage<BookVO> pageReaderBooks(IPage<BookVO> page,
                                  @Param("keyword") String keyword,
                                  @Param("categoryIds") List<Long> categoryIds,
                                  @Param("sort") String sort);

    @Select("SELECT b.*, p.publisher_name, c.category_name FROM base_book b " +
            "LEFT JOIN base_publisher p ON p.id = b.publisher_id " +
            "LEFT JOIN base_category c ON c.id = b.category_id WHERE b.isbn = #{isbn}")
    BookVO selectByIsbn(@Param("isbn") String isbn);
}