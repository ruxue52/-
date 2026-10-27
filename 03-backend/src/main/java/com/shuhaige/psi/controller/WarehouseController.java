package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseWarehouse;
import com.shuhaige.psi.mapper.BaseWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final BaseWarehouseMapper warehouseMapper;

    @GetMapping
    public Result<List<BaseWarehouse>> list() {
        return Result.ok(warehouseMapper.selectList(new LambdaQueryWrapper<BaseWarehouse>().orderByAsc(BaseWarehouse::getId)));
    }

    @GetMapping("/{id}")
    public Result<BaseWarehouse> get(@PathVariable Long id) {
        return Result.ok(warehouseMapper.selectById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody BaseWarehouse w) {
        w.setId(null);
        w.setCreateTime(LocalDateTime.now());
        warehouseMapper.insert(w);
        return Result.ok(w.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BaseWarehouse w) {
        w.setId(id);
        warehouseMapper.updateById(w);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseMapper.deleteById(id);
        return Result.ok();
    }
}