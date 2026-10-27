package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.PurchaseOrderDTO;
import com.shuhaige.psi.entity.PsiStockInLog;
import com.shuhaige.psi.service.PurchaseService;
import com.shuhaige.psi.vo.PurchaseOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase/orders")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public Result<PageResult<PurchaseOrderVO>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Long supplierId,
                                                    @RequestParam(required = false) Long warehouseId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate) {
        return Result.ok(purchaseService.page(page, size, status, supplierId, warehouseId, keyword, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<PurchaseOrderVO> detail(@PathVariable Long id) {
        return Result.ok(purchaseService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody PurchaseOrderDTO dto) {
        return Result.ok(purchaseService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PurchaseOrderDTO dto) {
        purchaseService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        purchaseService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        purchaseService.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/instock")
    public Result<Void> instock(@PathVariable Long id) {
        purchaseService.instock(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        purchaseService.cancel(id);
        return Result.ok();
    }

    @GetMapping("/{id}/logs")
    public Result<List<PsiStockInLog>> logs(@PathVariable Long id) {
        return Result.ok(purchaseService.logs(id));
    }
}