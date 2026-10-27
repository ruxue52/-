package com.shuhaige.psi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.entity.BaseCustomer;
import com.shuhaige.psi.entity.BaseMemberLevel;
import com.shuhaige.psi.mapper.BaseCustomerMapper;
import com.shuhaige.psi.mapper.BaseMemberLevelMapper;
import com.shuhaige.psi.security.LoginUser;
import com.shuhaige.psi.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 读者端（会员）资料查询与登录身份校验
 */
@Service
@RequiredArgsConstructor
public class ReaderMemberService {

    private final BaseCustomerMapper customerMapper;
    private final BaseMemberLevelMapper levelMapper;

    /** 按手机号匹配可登录的会员：须为在册客户且已登记手机号 */
    public BaseCustomer findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return customerMapper.selectOne(new LambdaQueryWrapper<BaseCustomer>()
                .eq(BaseCustomer::getPhone, phone.trim())
                .eq(BaseCustomer::getStatus, 1)
                .last("LIMIT 1"));
    }

    /** 取当前登录会员；非会员身份直接拒绝 */
    public BaseCustomer current() {
        LoginUser login = UserContext.get();
        if (login == null || !login.isCustomer()) {
            throw new BizException(ErrorCode.NO_PERMISSION, "请先使用会员手机号登录");
        }
        BaseCustomer customer = customerMapper.selectById(login.getUserId());
        if (customer == null) {
            throw BizException.notFound("会员资料不存在");
        }
        return customer;
    }

    /** 会员视图（对外字段，不含内部进价/成本等敏感数据） */
    public Map<String, Object> memberView(BaseCustomer c) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", c.getId());
        view.put("customerCode", c.getCustomerCode());
        view.put("customerName", c.getCustomerName());
        view.put("phone", c.getPhone());
        view.put("customerType", c.getCustomerType());
        view.put("address", c.getAddress());
        view.put("levelId", c.getLevelId());
        view.put("points", c.getPoints() == null ? 0 : c.getPoints());
        view.put("balance", c.getBalance() == null ? 0 : c.getBalance());

        BaseMemberLevel level = c.getLevelId() == null ? null : levelMapper.selectById(c.getLevelId());
        if (level == null) {
            view.put("levelCode", "NORMAL");
            view.put("levelName", "普通会员");
            view.put("discount", 100);
            view.put("pointRate", 1);
        } else {
            view.put("levelCode", level.getLevelCode());
            view.put("levelName", level.getLevelName());
            view.put("discount", level.getDiscount());
            view.put("pointRate", level.getPointRate());
        }
        return view;
    }
}
