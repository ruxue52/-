package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseSupplier;
import com.shuhaige.psi.mapper.BaseSupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final BaseSupplierMapper supplierMapper;

    @GetMapping
    public Result<PageResult<BaseSupplier>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String settleType) {
        LambdaQueryWrapper<BaseSupplier> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(BaseSupplier::getSupplierName, keyword).or().like(BaseSupplier::getSupplierCode, keyword));
        }
        if (settleType != null && !settleType.isEmpty()) {
            qw.eq(BaseSupplier::getSettleType, settleType);
        }
        qw.orderByAsc(BaseSupplier::getId);
        IPage<BaseSupplier> p = supplierMapper.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<BaseSupplier> get(@PathVariable Long id) {
        return Result.ok(supplierMapper.selectById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody BaseSupplier s) {
        s.setId(null);
        s.setCreateTime(LocalDateTime.now());
        supplierMapper.insert(s);
        return Result.ok(s.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BaseSupplier s) {
        s.setId(id);
        supplierMapper.updateById(s);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        supplierMapper.deleteById(id);
        return Result.ok();
    }
}