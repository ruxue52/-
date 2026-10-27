package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseCustomer;
import com.shuhaige.psi.entity.PsiStockOutItem;
import com.shuhaige.psi.mapper.PsiStockOutItemMapper;
import com.shuhaige.psi.mapper.PsiStockOutOrderMapper;
import com.shuhaige.psi.service.ReaderMemberService;
import com.shuhaige.psi.vo.SaleOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读者端（会员）中心：会员资料、消费记录（需 CUSTOMER 身份 token）
 */
@RestController
@RequestMapping("/reader/member")
@RequiredArgsConstructor
public class ReaderMemberController {

    /** 已出库的销售单才算会员消费记录 */
    private static final String STATUS_OUT = "OUTSTOCK";

    private final ReaderMemberService memberService;
    private final PsiStockOutOrderMapper outOrderMapper;
    private final PsiStockOutItemMapper outItemMapper;

    /** 会员资料 + 消费汇总 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        BaseCustomer customer = memberService.current();
        Map<String, Object> data = memberService.memberView(customer);
        Map<String, Object> sum = outOrderMapper.sumByCustomer(customer.getId());
        data.put("orderCount", sum == null ? 0 : sum.get("orderCount"));
        data.put("totalAmount", sum == null ? 0 : sum.get("totalAmount"));
        return Result.ok(data);
    }

    /** 我的消费记录 */
    @GetMapping("/orders")
    public Result<PageResult<SaleOrderVO>> orders(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        BaseCustomer customer = memberService.current();
        IPage<SaleOrderVO> p = outOrderMapper.pageOrders(new Page<>(page, size), STATUS_OUT,
                customer.getId(), null, null, null, null);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 消费记录明细 */
    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> orderDetail(@PathVariable Long id) {
        BaseCustomer customer = memberService.current();
        SaleOrderVO order = outOrderMapper.selectDetail(id);
        // 只允许查看本人的单据
        if (order == null || !customer.getId().equals(order.getCustomerId())) {
            throw new BizException(ErrorCode.NO_PERMISSION, "无权查看该单据");
        }
        List<PsiStockOutItem> items = outItemMapper.selectList(new LambdaQueryWrapper<PsiStockOutItem>()
                .eq(PsiStockOutItem::getOrderId, id).orderByAsc(PsiStockOutItem::getId));
        List<Map<String, Object>> itemViews = new ArrayList<>();
        for (PsiStockOutItem item : items) {
            Map<String, Object> iv = new LinkedHashMap<>();
            iv.put("bookId", item.getBookId());
            iv.put("bookName", item.getBookName());
            iv.put("isbn", item.getIsbn());
            iv.put("quantity", item.getQuantity());
            iv.put("price", item.getPrice());
            iv.put("amount", item.getAmount());
            itemViews.add(iv);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order", order);
        data.put("items", itemViews);
        return Result.ok(data);
    }
}
