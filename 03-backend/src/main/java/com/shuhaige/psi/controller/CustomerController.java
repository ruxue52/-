package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseCustomer;
import com.shuhaige.psi.mapper.BaseCustomerMapper;
import com.shuhaige.psi.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final BaseCustomerMapper customerMapper;

    @GetMapping
    public Result<PageResult<CustomerVO>> list(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String customerType,
                                               @RequestParam(required = false) String phone) {
        IPage<CustomerVO> p = customerMapper.pageCustomers(new Page<>(page, size), keyword, customerType, phone);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<BaseCustomer> get(@PathVariable Long id) {
        return Result.ok(customerMapper.selectById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody BaseCustomer c) {
        c.setId(null);
        c.setCreateTime(LocalDateTime.now());
        if (c.getPoints() == null) {
            c.setPoints(0);
        }
        customerMapper.insert(c);
        return Result.ok(c.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BaseCustomer c) {
        c.setId(id);
        customerMapper.updateById(c);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerMapper.deleteById(id);
        return Result.ok();
    }
}