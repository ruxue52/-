package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseBook;
import com.shuhaige.psi.entity.BaseCategory;
import com.shuhaige.psi.entity.BaseMemberLevel;
import com.shuhaige.psi.mapper.BaseBookMapper;
import com.shuhaige.psi.mapper.BaseCategoryMapper;
import com.shuhaige.psi.mapper.BaseMemberLevelMapper;
import com.shuhaige.psi.mapper.PsiStockMapper;
import com.shuhaige.psi.vo.BookVO;
import com.shuhaige.psi.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读者端（会员）图书浏览：图书检索、详情、门店库存、分类、会员等级
 * 全部为公开接口，返回字段已剔除进价/成本等内部数据
 */
@RestController
@RequestMapping("/reader")
@RequiredArgsConstructor
public class ReaderBookController {

    private final BaseBookMapper bookMapper;
    private final BaseCategoryMapper categoryMapper;
    private final BaseMemberLevelMapper levelMapper;
    private final PsiStockMapper stockMapper;

    /** 图书分页检索（仅在售） */
    @GetMapping("/books")
    public Result<PageResult<Map<String, Object>>> books(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) String sort) {
        IPage<BookVO> p = bookMapper.pageReaderBooks(new Page<>(page, size), keyword,
                expandCategory(categoryId), sort);
        List<Map<String, Object>> records = new ArrayList<>();
        for (BookVO book : p.getRecords()) {
            records.add(bookView(book));
        }
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), records));
    }

    /** 图书详情（含各门店在架数量） */
    @GetMapping("/books/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        BaseBook raw = bookMapper.selectById(id);
        BookVO book = raw == null ? null : bookMapper.selectByIsbn(raw.getIsbn());
        return Result.ok(detailView(book));
    }

    /** 扫码 / 手输 ISBN 查书 */
    @GetMapping("/books/by-isbn/{isbn}")
    public Result<Map<String, Object>> byIsbn(@PathVariable String isbn) {
        return Result.ok(detailView(bookMapper.selectByIsbn(isbn.trim())));
    }

    /** 图书分类（树形，仅启用分类） */
    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        List<BaseCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<BaseCategory>()
                .eq(BaseCategory::getStatus, 1)
                .orderByAsc(BaseCategory::getSortNo).orderByAsc(BaseCategory::getId));
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (BaseCategory c : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", c.getId());
            node.put("parentId", c.getParentId());
            node.put("categoryName", c.getCategoryName());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(c.getId(), node);
        }
        List<Map<String, Object>> tree = new ArrayList<>();
        for (BaseCategory c : all) {
            Map<String, Object> node = nodes.get(c.getId());
            Map<String, Object> parent = c.getParentId() == null ? null : nodes.get(c.getParentId());
            if (parent != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(node);
            } else {
                tree.add(node);
            }
        }
        return Result.ok(tree);
    }

    /** 会员等级与权益 */
    @GetMapping("/levels")
    public Result<List<BaseMemberLevel>> levels() {
        return Result.ok(levelMapper.selectList(new LambdaQueryWrapper<BaseMemberLevel>()
                .orderByAsc(BaseMemberLevel::getDiscount).orderByAsc(BaseMemberLevel::getId)));
    }

    /** 选中父分类时，一并纳入其子分类 */
    private List<Long> expandCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        List<BaseCategory> children = categoryMapper.selectList(new LambdaQueryWrapper<BaseCategory>()
                .eq(BaseCategory::getParentId, categoryId).eq(BaseCategory::getStatus, 1));
        for (BaseCategory child : children) {
            ids.add(child.getId());
        }
        return ids;
    }

    /** 详情视图：图书信息 + 各门店在架数量 */
    private Map<String, Object> detailView(BookVO book) {
        if (book == null || (book.getStatus() != null && book.getStatus() != 1)) {
            throw BizException.notFound("未找到该图书，或该图书已下架");
        }
        List<StockVO> stocks = stockMapper.listByBook(book.getId());
        List<Map<String, Object>> stockViews = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (StockVO s : stocks) {
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            Map<String, Object> sv = new LinkedHashMap<>();
            sv.put("warehouseId", s.getWarehouseId());
            sv.put("warehouseName", s.getWarehouseName());
            sv.put("quantity", qty);
            stockViews.add(sv);
            total = total.add(qty);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book", bookView(book));
        data.put("stock", stockViews);
        data.put("totalQuantity", total);
        data.put("available", total.compareTo(BigDecimal.ZERO) > 0);
        return data;
    }

    /** 对外图书字段（不含 purchasePrice 等内部数据） */
    private Map<String, Object> bookView(BookVO b) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", b.getId());
        v.put("isbn", b.getIsbn());
        v.put("bookName", b.getBookName());
        v.put("author", b.getAuthor());
        v.put("publisherName", b.getPublisherName());
        v.put("categoryId", b.getCategoryId());
        v.put("categoryName", b.getCategoryName());
        v.put("publishDate", b.getPublishDate());
        v.put("price", b.getPrice());
        v.put("salePrice", b.getSalePrice());
        v.put("unit", b.getUnit());
        v.put("shelfNo", b.getShelfNo());
        v.put("coverUrl", b.getCoverUrl());
        v.put("summary", b.getSummary());
        return v;
    }
}
