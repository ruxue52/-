package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.SaleOrderDTO;
import com.shuhaige.psi.service.SaleService;
import com.shuhaige.psi.vo.SaleOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sale")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping("/orders")
    public Result<PageResult<SaleOrderVO>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long customerId,
                                                @RequestParam(required = false) Long warehouseId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        return Result.ok(saleService.page(page, size, status, customerId, warehouseId, keyword, startDate, endDate));
    }

    @GetMapping("/orders/{id}")
    public Result<SaleOrderVO> detail(@PathVariable Long id) {
        return Result.ok(saleService.detail(id));
    }

    @PostMapping("/orders")
    public Result<Long> create(@RequestBody SaleOrderDTO dto) {
        return Result.ok(saleService.create(dto));
    }

    @PutMapping("/orders/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SaleOrderDTO dto) {
        saleService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/orders/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        saleService.delete(id);
        return Result.ok();
    }

    @PostMapping("/orders/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        saleService.submit(id);
        return Result.ok();
    }

    @PostMapping("/orders/{id}/outstock")
    public Result<Void> outstock(@PathVariable Long id) {
        saleService.outstock(id);
        return Result.ok();
    }

    @PostMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        saleService.cancel(id);
        return Result.ok();
    }

    @GetMapping("/pos/search")
    public Result<List<Map<String, Object>>> posSearch(@RequestParam String keyword,
                                                       @RequestParam(required = false) Long warehouseId) {
        return Result.ok(saleService.posSearch(keyword, warehouseId));
    }
}