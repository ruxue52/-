package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.ReturnOrderDTO;
import com.shuhaige.psi.entity.PsiReturnOrder;
import com.shuhaige.psi.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/returns")
@RequiredArgsConstructor
public class ReturnsController {

    private final StockService stockService;

    @GetMapping
    public Result<PageResult<PsiReturnOrder>> list(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String returnType,
                                                   @RequestParam(required = false) String keyword) {
        return Result.ok(stockService.returnPage(page, size, status, returnType, keyword));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(stockService.returnDetail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody ReturnOrderDTO dto) {
        return Result.ok(stockService.createReturn(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ReturnOrderDTO dto) {
        stockService.updateReturn(id, dto);
        return Result.ok();
    }

    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id) {
        stockService.finishReturn(id);
        return Result.ok();
    }
}
