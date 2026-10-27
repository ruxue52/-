package com.shuhaige.psi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuhaige.psi.entity.BaseCustomer;
import com.shuhaige.psi.vo.CustomerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BaseCustomerMapper extends BaseMapper<BaseCustomer> {

    @Select("<script>SELECT c.*, l.level_name, l.discount FROM base_customer c " +
            "LEFT JOIN base_member_level l ON l.id = c.level_id <where>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (c.customer_name LIKE CONCAT('%',#{keyword},'%') OR c.customer_code LIKE CONCAT('%',#{keyword},'%') OR c.phone LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test=\"customerType != null and customerType != ''\"> AND c.customer_type = #{customerType}</if>" +
            "<if test=\"phone != null and phone != ''\"> AND c.phone LIKE CONCAT('%',#{phone},'%')</if>" +
            "</where> ORDER BY c.id</script>")
    IPage<CustomerVO> pageCustomers(IPage<CustomerVO> page,
                                    @Param("keyword") String keyword,
                                    @Param("customerType") String customerType,
                                    @Param("phone") String phone);
}