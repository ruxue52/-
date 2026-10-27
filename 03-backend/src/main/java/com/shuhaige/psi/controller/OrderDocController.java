package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.OrderType;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.OrderArchiveDTO;
import com.shuhaige.psi.dto.RevisionApplyDTO;
import com.shuhaige.psi.dto.RevisionAuditDTO;
import com.shuhaige.psi.entity.PsiOrderActionLog;
import com.shuhaige.psi.entity.PsiOrderRevision;
import com.shuhaige.psi.security.RequirePerm;
import com.shuhaige.psi.service.OrderDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 单据确认锁定与修订审批的统一入口（按 {orderType} 区分 IN/OUT/CHECK/TRANSFER/RETURN）。
 * <p>路由无冲突：/orders/revisions/pending 为 2 段；/{orderType}/{id}/xxx 为 3 段。
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderDocController {

    private final OrderDocService orderDocService;

    @GetMapping("/{orderType}/{id}/logs")
    public Result<List<PsiOrderActionLog>> logs(@PathVariable String orderType, @PathVariable Long id) {
        return Result.ok(orderDocService.actions(OrderType.of(orderType), id));
    }

    @GetMapping("/{orderType}/{id}/revisions")
    public Result<List<PsiOrderRevision>> revisions(@PathVariable String orderType, @PathVariable Long id) {
        return Result.ok(orderDocService.revisions(OrderType.of(orderType), id));
    }

    @PostMapping("/{orderType}/{id}/confirm")
    @RequirePerm("order:confirm")
    public Result<Void> confirm(@PathVariable String orderType, @PathVariable Long id) {
        orderDocService.confirm(OrderType.of(orderType), id);
        return Result.ok();
    }

    @PostMapping("/{orderType}/{id}/revision")
    @RequirePerm("order:revision:apply")
    public Result<Long> applyRevision(@PathVariable String orderType, @PathVariable Long id,
                                      @RequestBody RevisionApplyDTO dto) {
        return Result.ok(orderDocService.applyRevision(OrderType.of(orderType), id, dto.getReason()));
    }

    @GetMapping("/revisions/pending")
    @RequirePerm("order:revision:approve")
    public Result<PageResult<PsiOrderRevision>> pendingRevisions(@RequestParam(defaultValue = "1") long page,
                                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderDocService.pendingRevisions(page, size));
    }

    @PostMapping("/revisions/{revisionId}/approve")
    @RequirePerm("order:revision:approve")
    public Result<Void> approve(@PathVariable Long revisionId,
                                @RequestBody(required = false) RevisionAuditDTO dto) {
        orderDocService.approveRevision(revisionId, dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    @PostMapping("/revisions/{revisionId}/reject")
    @RequirePerm("order:revision:approve")
    public Result<Void> reject(@PathVariable Long revisionId,
                               @RequestBody(required = false) RevisionAuditDTO dto) {
        orderDocService.rejectRevision(revisionId, dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    @PostMapping("/revisions/{revisionId}/cancel")
    @RequirePerm("order:revision:apply")
    public Result<Void> cancel(@PathVariable Long revisionId) {
        orderDocService.cancelRevision(revisionId);
        return Result.ok();
    }

    // ---------------- 归档 ----------------

    @PostMapping("/{orderType}/{id}/archive")
    @RequirePerm("order:archive")
    public Result<Void> archive(@PathVariable String orderType, @PathVariable Long id) {
        orderDocService.archive(OrderType.of(orderType), id);
        return Result.ok();
    }

    @GetMapping("/archive/summary")
    @RequirePerm("order:archive")
    public Result<List<Map<String, Object>>> archiveSummary(
            @RequestParam(defaultValue = "180") int keepDays) {
        return Result.ok(orderDocService.archiveSummary(keepDays));
    }

    @PostMapping("/archive/batch")
    @RequirePerm("order:archive")
    public Result<Integer> archiveBatch(@RequestBody OrderArchiveDTO dto) {
        int days = dto.getKeepDays() == null
                ? OrderDocService.DEFAULT_KEEP_DAYS : dto.getKeepDays();
        return Result.ok(orderDocService.archiveBatch(OrderType.of(dto.getOrderType()), days));
    }

    @GetMapping("/{orderType}/archived")
    @RequirePerm("order:archive")
    public Result<PageResult<?>> archived(@PathVariable String orderType,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(orderDocService.archivedPage(OrderType.of(orderType), page, size, keyword));
    }
}
