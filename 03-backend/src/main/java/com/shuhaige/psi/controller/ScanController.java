package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.ScanDTO;
import com.shuhaige.psi.entity.*;
import com.shuhaige.psi.mapper.*;
import com.shuhaige.psi.security.UserContext;
import com.shuhaige.psi.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 扫码解析与记录
 */
@RestController
@RequestMapping("/scan")
@RequiredArgsConstructor
public class ScanController {

    private final BaseBookMapper bookMapper;
    private final PsiStockMapper stockMapper;
    private final PsiStockInOrderMapper inOrderMapper;
    private final PsiStockOutOrderMapper outOrderMapper;
    private final PsiStockCheckOrderMapper checkOrderMapper;
    private final SysScanLogMapper scanLogMapper;

    @PostMapping("/resolve")
    public Result<Map<String, Object>> resolve(@RequestBody ScanDTO dto) {
        String code = dto.getCode() == null ? "" : dto.getCode().trim();
        Map<String, Object> result = new HashMap<>();
        String bizType = "UNKNOWN";
        Object entity = null;
        Map<String, Object> extra = new HashMap<>();

        String[] parts = code.split(":");
        if (code.startsWith("SHG:BOOK:") && parts.length >= 3) {
            bizType = "BOOK";
            String isbn = parts[2];
            BookVO book = bookMapper.selectByIsbn(isbn);
            entity = book;
            if (book != null) {
                extra.put("stocks", stockMapper.listByBook(book.getId()));
            }
        } else if (code.startsWith("SHG:ORDER:") && parts.length >= 3) {
            bizType = "ORDER";
            String orderNo = parts[2];
            PsiStockInOrder inOrder = inOrderMapper.selectOne(new LambdaQueryWrapper<PsiStockInOrder>()
                    .eq(PsiStockInOrder::getOrderNo, orderNo));
            if (inOrder != null) {
                entity = inOrder;
                extra.put("orderType", "PURCHASE");
            } else {
                PsiStockOutOrder outOrder = outOrderMapper.selectOne(new LambdaQueryWrapper<PsiStockOutOrder>()
                        .eq(PsiStockOutOrder::getOrderNo, orderNo));
                entity = outOrder;
                extra.put("orderType", "SALE");
            }
        } else if (code.startsWith("SHG:STOCK:") && parts.length >= 4) {
            bizType = "STOCK";
            extra.put("warehouseId", parts[2]);
            entity = bookMapper.selectByIsbn(parts[3]);
        } else if (code.startsWith("SHG:CHECK:") && parts.length >= 4) {
            bizType = "CHECK";
            extra.put("isbn", parts[3]);
            entity = checkOrderMapper.selectOne(new LambdaQueryWrapper<PsiStockCheckOrder>()
                    .eq(PsiStockCheckOrder::getCheckNo, parts[2]));
        }
        result.put("bizType", bizType);
        result.put("entity", entity);
        result.put("extra", extra);
        return Result.ok(result);
    }

    @PostMapping("/log")
    public Result<Void> log(@RequestBody ScanDTO dto) {
        SysScanLog log = new SysScanLog();
        log.setCode(dto.getCode());
        log.setBizType(dto.getBizType());
        log.setBizId(dto.getBizId());
        log.setDevice(dto.getDevice() == null ? "APP" : dto.getDevice());
        log.setUserId(UserContext.getUserId());
        log.setResult(dto.getResult() == null ? "SUCCESS" : dto.getResult());
        log.setCreateTime(LocalDateTime.now());
        scanLogMapper.insert(log);
        return Result.ok();
    }
}