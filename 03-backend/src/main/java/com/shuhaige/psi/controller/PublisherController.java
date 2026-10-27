package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BasePublisher;
import com.shuhaige.psi.mapper.BasePublisherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/publishers")
@RequiredArgsConstructor
public class PublisherController {

    private final BasePublisherMapper publisherMapper;

    @GetMapping
    public Result<PageResult<BasePublisher>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BasePublisher> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(BasePublisher::getPublisherName, keyword);
        }
        qw.orderByAsc(BasePublisher::getId);
        IPage<BasePublisher> p = publisherMapper.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<BasePublisher> get(@PathVariable Long id) {
        return Result.ok(publisherMapper.selectById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody BasePublisher p) {
        p.setId(null);
        p.setCreateTime(LocalDateTime.now());
        publisherMapper.insert(p);
        return Result.ok(p.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BasePublisher p) {
        p.setId(id);
        publisherMapper.updateById(p);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        publisherMapper.deleteById(id);
        return Result.ok();
    }
}