package com.shuhaige.psi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseBook;
import com.shuhaige.psi.mapper.BaseBookMapper;
import com.shuhaige.psi.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BaseBookMapper bookMapper;

    @GetMapping
    public Result<PageResult<BookVO>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Long publisherId,
                                           @RequestParam(required = false) Integer status) {
        IPage<BookVO> p = bookMapper.pageBooks(new Page<>(page, size), keyword, categoryId, publisherId, status);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @GetMapping("/by-isbn/{isbn}")
    public Result<BookVO> byIsbn(@PathVariable String isbn) {
        BookVO vo = bookMapper.selectByIsbn(isbn);
        if (vo == null) {
            throw BizException.notFound("图书不存在");
        }
        return Result.ok(vo);
    }

    @GetMapping("/{id}")
    public Result<BookVO> get(@PathVariable Long id) {
        BaseBook book = bookMapper.selectById(id);
        if (book == null) {
            throw BizException.notFound("图书不存在");
        }
        BookVO vo = bookMapper.selectByIsbn(book.getIsbn());
        return Result.ok(vo);
    }

    @PostMapping
    public Result<Long> create(@RequestBody BaseBook book) {
        Long exists = bookMapper.selectCount(new LambdaQueryWrapper<BaseBook>().eq(BaseBook::getIsbn, book.getIsbn()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "ISBN 已存在");
        }
        book.setId(null);
        book.setCreateTime(LocalDateTime.now());
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.insert(book);
        return Result.ok(book.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BaseBook book) {
        BaseBook db = bookMapper.selectById(id);
        if (db == null) {
            throw BizException.notFound("图书不存在");
        }
        book.setId(id);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(book);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookMapper.deleteById(id);
        return Result.ok();
    }
}