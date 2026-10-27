package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseMemberLevel;
import com.shuhaige.psi.mapper.BaseMemberLevelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member-levels")
@RequiredArgsConstructor
public class MemberLevelController {

    private final BaseMemberLevelMapper levelMapper;

    @GetMapping
    public Result<List<BaseMemberLevel>> list() {
        return Result.ok(levelMapper.selectList(new LambdaQueryWrapper<BaseMemberLevel>().orderByAsc(BaseMemberLevel::getId)));
    }

    @GetMapping("/{id}")
    public Result<BaseMemberLevel> get(@PathVariable Long id) {
        return Result.ok(levelMapper.selectById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody BaseMemberLevel l) {
        l.setId(null);
        levelMapper.insert(l);
        return Result.ok(l.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BaseMemberLevel l) {
        l.setId(id);
        levelMapper.updateById(l);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        levelMapper.deleteById(id);
        return Result.ok();
    }
}