package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.CheckOrderDTO;
import com.shuhaige.psi.dto.TransferOrderDTO;
import com.shuhaige.psi.entity.PsiStockCheckOrder;
import com.shuhaige.psi.entity.PsiStockTransferOrder;
import com.shuhaige.psi.service.StockService;
import com.shuhaige.psi.vo.StockFlowVO;
import com.shuhaige.psi.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/list")
    public Result<PageResult<StockVO>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Long warehouseId,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "false") boolean onlyLow) {
        return Result.ok(stockService.stockList(page, size, warehouseId, categoryId, keyword, onlyLow));
    }

    @GetMapping("/by-book/{bookId}")
    public Result<List<StockVO>> byBook(@PathVariable Long bookId) {
        return Result.ok(stockService.stockByBook(bookId));
    }

    @GetMapping("/warnings")
    public Result<List<StockVO>> warnings() {
        return Result.ok(stockService.warnings());
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(stockService.summary());
    }

    @GetMapping("/flows")
    public Result<PageResult<StockFlowVO>> flows(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) Long bookId,
                                                 @RequestParam(required = false) Long warehouseId,
                                                 @RequestParam(required = false) String bizType,
                                                 @RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate) {
        return Result.ok(stockService.flows(page, size, bookId, warehouseId, bizType, startDate, endDate));
    }

    // ---------------- 盘点 ----------------

    @PostMapping("/check/orders")
    public Result<Long> createCheck(@RequestBody CheckOrderDTO dto) {
        return Result.ok(stockService.createCheck(dto));
    }

    @GetMapping("/check/orders")
    public Result<PageResult<PsiStockCheckOrder>> checkList(@RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "10") long size,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) Long warehouseId) {
        return Result.ok(stockService.checkPage(page, size, status, warehouseId));
    }

    @GetMapping("/check/orders/{id}")
    public Result<Map<String, Object>> checkDetail(@PathVariable Long id) {
        return Result.ok(stockService.checkDetail(id));
    }

    @PutMapping("/check/orders/{id}/items")
    public Result<Void> updateCheckItems(@PathVariable Long id, @RequestBody CheckOrderDTO.ItemsDTO dto) {
        stockService.updateCheckItems(id, dto.getItems());
        return Result.ok();
    }

    @PostMapping("/check/orders/{id}/finish")
    public Result<Void> finishCheck(@PathVariable Long id) {
        stockService.finishCheck(id);
        return Result.ok();
    }

    @PostMapping("/check/orders/{id}/cancel")
    public Result<Void> cancelCheck(@PathVariable Long id) {
        stockService.cancelCheck(id);
        return Result.ok();
    }

    // ---------------- 调拨 ----------------

    @PostMapping("/transfer/orders")
    public Result<Long> createTransfer(@RequestBody TransferOrderDTO dto) {
        return Result.ok(stockService.createTransfer(dto));
    }

    @GetMapping("/transfer/orders")
    public Result<PageResult<PsiStockTransferOrder>> transferList(@RequestParam(defaultValue = "1") long page,
                                                                  @RequestParam(defaultValue = "10") long size,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String keyword) {
        return Result.ok(stockService.transferPage(page, size, status, keyword));
    }

    @GetMapping("/transfer/orders/{id}")
    public Result<Map<String, Object>> transferDetail(@PathVariable Long id) {
        return Result.ok(stockService.transferDetail(id));
    }

    @PutMapping("/transfer/orders/{id}")
    public Result<Void> updateTransfer(@PathVariable Long id, @RequestBody TransferOrderDTO dto) {
        stockService.updateTransfer(id, dto);
        return Result.ok();
    }

    @PostMapping("/transfer/orders/{id}/transfer")
    public Result<Void> transfer(@PathVariable Long id) {
        stockService.executeTransfer(id);
        return Result.ok();
    }
}