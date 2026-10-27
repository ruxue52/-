package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.MobileCheckDTO;
import com.shuhaige.psi.entity.PsiStockCheckItem;
import com.shuhaige.psi.entity.PsiStockCheckOrder;
import com.shuhaige.psi.mapper.PsiStockCheckItemMapper;
import com.shuhaige.psi.mapper.PsiStockCheckOrderMapper;
import com.shuhaige.psi.mapper.PsiStockMapper;
import com.shuhaige.psi.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 移动端：首页指标 / 快速查询 / 扫码盘点
 */
@RestController
@RequestMapping("/mobile")
@RequiredArgsConstructor
public class MobileController {

    private final ReportMapper reportMapper;
    private final PsiStockMapper stockMapper;
    private final PsiStockCheckOrderMapper checkOrderMapper;
    private final PsiStockCheckItemMapper checkItemMapper;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(reportMapper.overview());
    }

    @GetMapping("/stock/quick")
    public Result<List<Map<String, Object>>> stockQuick(@RequestParam String keyword) {
        return Result.ok(stockMapper.posSearch(keyword, null));
    }

    @GetMapping("/check/pending/{checkId}")
    public Result<List<PsiStockCheckItem>> pending(@PathVariable Long checkId) {
        return Result.ok(checkItemMapper.selectList(new LambdaQueryWrapper<PsiStockCheckItem>()
                .eq(PsiStockCheckItem::getOrderId, checkId)
                .isNull(PsiStockCheckItem::getScanCode)
                .orderByAsc(PsiStockCheckItem::getId)));
    }

    @PostMapping("/check/scan")
    public Result<PsiStockCheckItem> scan(@RequestBody MobileCheckDTO dto) {
        PsiStockCheckOrder order = checkOrderMapper.selectById(dto.getCheckId());
        if (order == null) {
            throw BizException.notFound("盘点单不存在");
        }
        String isbn = extractIsbn(dto.getCode());
        PsiStockCheckItem item = checkItemMapper.selectOne(new LambdaQueryWrapper<PsiStockCheckItem>()
                .eq(PsiStockCheckItem::getOrderId, dto.getCheckId())
                .eq(PsiStockCheckItem::getIsbn, isbn));
        if (item == null) {
            throw BizException.notFound("盘点明细不存在: " + isbn);
        }
        BigDecimal realQty = dto.getRealQty() == null ? item.getBookQty() : dto.getRealQty();
        item.setRealQty(realQty);
        item.setDiffQty(realQty.subtract(item.getBookQty()));
        item.setScanCode(dto.getCode());
        checkItemMapper.updateById(item);
        return Result.ok(item);
    }

    @GetMapping("/stat/summary")
    public Result<Map<String, Object>> statSummary() {
        return Result.ok(reportMapper.overview());
    }

    private String extractIsbn(String code) {
        if (code == null) {
            return "";
        }
        String[] parts = code.split(":");
        if (code.startsWith("SHG:")) {
            return parts[parts.length - 1];
        }
        return code;
    }
}