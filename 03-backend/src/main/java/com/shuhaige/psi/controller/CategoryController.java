package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseCategory;
import com.shuhaige.psi.mapper.BaseCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final BaseCategoryMapper categoryMapper;

    /** 返回树形数组 */
    @GetMapping
    public Result<List<Map<String, Object>>> tree() {
        List<BaseCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<BaseCategory>()
                .orderByAsc(BaseCategory::getSortNo).orderByAsc(BaseCategory::getId));
        Map<Long, Map<String, Object>> map = new LinkedHashMap<>();
        for (BaseCategory c : all) {
            map.put(c.getId(), node(c));
        }
        List<Map<String, Object>> tree = new ArrayList<>();
        for (BaseCategory c : all) {
            Map<String, Object> n = map.get(c.getId());
            Map<String, Object> parent = map.get(c.getParentId());
            if (c.getParentId() != null && c.getParentId() != 0 && parent != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(n);
            } else {
                tree.add(n);
            }
        }
        return Result.ok(tree);
    }

    private Map<String, Object> node(BaseCategory c) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", c.getId());
        n.put("parentId", c.getParentId());
        n.put("categoryName", c.getCategoryName());
        n.put("categoryCode", c.getCategoryCode());
        n.put("sortNo", c.getSortNo());
        n.put("status", c.getStatus());
        n.put("children", new ArrayList<Map<String, Object>>());
        return n;
    }

    @GetMapping("/{id}")
    public Result<BaseCategory> get(@PathVariable Long id) {
        return Result.ok(categoryMapper.selectById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody BaseCategory c) {
        c.setId(null);
        c.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(c);
        return Result.ok(c.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BaseCategory c) {
        c.setId(id);
        categoryMapper.updateById(c);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.ok();
    }
}