package com.shuhaige.psi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.OrderType;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.entity.*;
import com.shuhaige.psi.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单据确认锁定 + 修订审批的核心服务。
 * <p>语义：确认 = 置 is_locked=1（不改 status）；修订 = 申请 -> 审批解锁 -> 修订 -> 重新确认。
 * <p>已执行/已作废的终态单据禁止申请修订，纠错请走反向单据。
 */
@Service
@RequiredArgsConstructor
public class OrderDocService {

    private final PsiStockInOrderMapper inOrderMapper;
    private final PsiStockOutOrderMapper outOrderMapper;
    private final PsiStockCheckOrderMapper checkOrderMapper;
    private final PsiStockTransferOrderMapper transferOrderMapper;
    private final PsiReturnOrderMapper returnOrderMapper;
    private final PsiOrderRevisionMapper revisionMapper;
    private final PsiOrderActionLogMapper actionLogMapper;
    private final OrderSnapshotService snapshotService;
    private final CurrentOperator operator;

    /** 锁定即拒绝的统一文案 */
    public static final String LOCKED_MSG = "单据已确认锁定，如需修改请先申请修订";

    // ---------------- 确认锁定 ----------------

    @Transactional
    public void confirm(OrderType type, Long id) {
        LockAware order = load(type, id);
        if (isLocked(order)) {
            throw BizException.status("单据已确认锁定，请勿重复确认");
        }
        if (type.isTerminal(order.getStatus())) {
            throw BizException.status("单据已结束，不可确认");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setIsLocked(1);
        order.setConfirmUserId(operator.id());
        order.setConfirmUserName(operator.name());
        order.setConfirmTime(now);
        save(type, order);

        // 若存在已批准解锁的在途修订，本次确认即闭环：采集修改后快照并关闭
        List<PsiOrderRevision> approved = revisionMapper.selectList(new LambdaQueryWrapper<PsiOrderRevision>()
                .eq(PsiOrderRevision::getOrderType, type.name())
                .eq(PsiOrderRevision::getOrderId, id)
                .eq(PsiOrderRevision::getStatus, "APPROVED")
                .orderByDesc(PsiOrderRevision::getId));
        if (!approved.isEmpty()) {
            PsiOrderRevision rev = approved.get(0);
            rev.setAfterSnapshot(snapshotService.capture(type, id));
            rev.setStatus("CLOSED");
            rev.setCloseTime(now);
            rev.setUpdateTime(now);
            revisionMapper.updateById(rev);
        }
        recordAction(type, id, order.bizNo(), "CONFIRM", "确认锁定单据");
    }

    // ---------------- 修订申请与审批 ----------------

    @Transactional
    public Long applyRevision(OrderType type, Long id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "修订原因不能为空");
        }
        LockAware order = load(type, id);
        if (type.isTerminal(order.getStatus())) {
            throw BizException.status("单据已执行或作废，不可申请修订，请改走反向单据");
        }
        if (!isLocked(order)) {
            throw BizException.status("单据尚未锁定，可直接修改无需申请");
        }
        Long inFlight = revisionMapper.selectCount(new LambdaQueryWrapper<PsiOrderRevision>()
                .eq(PsiOrderRevision::getOrderType, type.name())
                .eq(PsiOrderRevision::getOrderId, id)
                .in(PsiOrderRevision::getStatus, "PENDING", "APPROVED"));
        if (inFlight != null && inFlight > 0) {
            throw BizException.status("该单据已有在途修订申请，请勿重复申请");
        }
        LocalDateTime now = LocalDateTime.now();
        PsiOrderRevision rev = new PsiOrderRevision();
        rev.setOrderType(type.name());
        rev.setOrderId(id);
        rev.setOrderNo(order.bizNo());
        rev.setReason(reason.trim());
        rev.setStatus("PENDING");
        rev.setApplyUserId(operator.id());
        rev.setApplyUserName(operator.name());
        rev.setApplyTime(now);
        rev.setCreateTime(now);
        rev.setUpdateTime(now);
        revisionMapper.insert(rev);
        recordAction(type, id, order.bizNo(), "REVISION_APPLY", "申请修订：" + rev.getReason());
        return rev.getId();
    }

    @Transactional
    public void approveRevision(Long revisionId, String remark) {
        PsiOrderRevision rev = requirePending(revisionId);
        Long uid = operator.id();
        if (uid != null && uid.equals(rev.getApplyUserId())) {
            throw BizException.status("审批人不能与申请人相同（四眼原则）");
        }
        OrderType type = OrderType.of(rev.getOrderType());
        LockAware order = load(type, rev.getOrderId());
        if (type.isTerminal(order.getStatus())) {
            throw BizException.status("单据已结束，不可修订");
        }
        // 解锁前采集「修改前」快照：锁定期间内容未被改过，等价于修改前状态
        String before = snapshotService.capture(type, rev.getOrderId());
        unlock(type, rev.getOrderId());

        rev.setBeforeSnapshot(before);
        rev.setStatus("APPROVED");
        rev.setApproveUserId(uid);
        rev.setApproveUserName(operator.name());
        rev.setApproveTime(LocalDateTime.now());
        rev.setApproveRemark(remark);
        rev.setUpdateTime(LocalDateTime.now());
        int updated = revisionMapper.update(rev, new LambdaQueryWrapper<PsiOrderRevision>()
                .eq(PsiOrderRevision::getId, revisionId)
                .eq(PsiOrderRevision::getStatus, "PENDING"));
        if (updated == 0) {
            throw BizException.status("该修订申请已被处理");
        }
        recordAction(type, rev.getOrderId(), rev.getOrderNo(), "REVISION_APPROVE",
                "批准修订并解锁" + (isBlank(remark) ? "" : "：" + remark));
    }

    @Transactional
    public void rejectRevision(Long revisionId, String remark) {
        PsiOrderRevision rev = requirePending(revisionId);
        Long uid = operator.id();
        if (uid != null && uid.equals(rev.getApplyUserId())) {
            throw BizException.status("审批人不能与申请人相同（四眼原则）");
        }
        rev.setStatus("REJECTED");
        rev.setApproveUserId(uid);
        rev.setApproveUserName(operator.name());
        rev.setApproveTime(LocalDateTime.now());
        rev.setApproveRemark(remark);
        rev.setUpdateTime(LocalDateTime.now());
        int updated = revisionMapper.update(rev, new LambdaQueryWrapper<PsiOrderRevision>()
                .eq(PsiOrderRevision::getId, revisionId)
                .eq(PsiOrderRevision::getStatus, "PENDING"));
        if (updated == 0) {
            throw BizException.status("该修订申请已被处理");
        }
        OrderType type = OrderType.of(rev.getOrderType());
        recordAction(type, rev.getOrderId(), rev.getOrderNo(), "REVISION_REJECT",
                "驳回修订" + (isBlank(remark) ? "" : "：" + remark));
    }

    @Transactional
    public void cancelRevision(Long revisionId) {
        PsiOrderRevision rev = requirePending(revisionId);
        Long uid = operator.id();
        if (uid != null && !uid.equals(rev.getApplyUserId())) {
            throw BizException.status("只能撤销本人发起的修订申请");
        }
        rev.setStatus("CANCELLED");
        rev.setUpdateTime(LocalDateTime.now());
        int updated = revisionMapper.update(rev, new LambdaQueryWrapper<PsiOrderRevision>()
                .eq(PsiOrderRevision::getId, revisionId)
                .eq(PsiOrderRevision::getStatus, "PENDING"));
        if (updated == 0) {
            throw BizException.status("该修订申请已被处理");
        }
        recordAction(OrderType.of(rev.getOrderType()), rev.getOrderId(), rev.getOrderNo(),
                "REVISION_CANCEL", "撤销修订申请");
    }

    // ---------------- 归档 ----------------

    /** 保留期默认天数 */
    public static final int DEFAULT_KEEP_DAYS = 180;

    /**
     * 单据归档：仅「终态 + 已确认锁定 + 未归档」可归档。
     * 归档为逻辑标记，不删除数据；归档后常规列表不再显示，仅归档页可查，且单据只读。
     */
    @Transactional
    public void archive(OrderType type, Long id) {
        LockAware order = load(type, id);
        if (!type.isTerminal(order.getStatus())) {
            throw BizException.status("仅已执行或已作废的单据可归档");
        }
        if (!isLocked(order)) {
            throw BizException.status("单据未确认锁定，不可归档");
        }
        if (isArchived(order)) {
            throw BizException.status("单据已归档，请勿重复归档");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setIsArchived(1);
        order.setArchiveTime(now);
        order.setArchiveUserId(operator.id());
        order.setArchiveUserName(operator.name());
        save(type, order);
        recordAction(type, id, order.bizNo(), "ARCHIVE", "单据归档");
    }

    /**
     * 批量归档：归档某类型下「终态 + 已锁定 + 未归档」且创建时间早于 keepDays 天的单据。
     * 逐单归档以便为每张单据留一条 ARCHIVE 流水。
     */
    @Transactional
    public int archiveBatch(OrderType type, int keepDays) {
        if (keepDays < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "保留天数不能为负数");
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
        List<Long> ids = candidateIds(type, cutoff);
        for (Long id : ids) {
            archive(type, id);
        }
        return ids.size();
    }

    /** 某类型的待归档数量（终态 + 已锁定 + 未归档 + 创建时间早于保留期） */
    public long archiveCandidates(OrderType type, int keepDays) {
        int days = keepDays < 0 ? DEFAULT_KEEP_DAYS : keepDays;
        return candidateIds(type, LocalDateTime.now().minusDays(days)).size();
    }

    /** 全部类型的归档概览：待归档数 + 已归档数 */
    public List<Map<String, Object>> archiveSummary(int keepDays) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderType type : OrderType.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderType", type.name());
            m.put("label", type.label());
            m.put("candidates", archiveCandidates(type, keepDays));
            m.put("archivedTotal", archivedCount(type));
            list.add(m);
        }
        return list;
    }

    /** 归档单据分页查询 */
    public PageResult<?> archivedPage(OrderType type, long page, long size, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        IPage<?> p;
        switch (type) {
            case IN:
                p = inOrderMapper.selectPage(new Page<PsiStockInOrder>(page, size),
                        new LambdaQueryWrapper<PsiStockInOrder>()
                                .eq(PsiStockInOrder::getIsArchived, 1)
                                .like(hasKeyword, PsiStockInOrder::getOrderNo, keyword)
                                .orderByDesc(PsiStockInOrder::getArchiveTime));
                break;
            case OUT:
                p = outOrderMapper.selectPage(new Page<PsiStockOutOrder>(page, size),
                        new LambdaQueryWrapper<PsiStockOutOrder>()
                                .eq(PsiStockOutOrder::getIsArchived, 1)
                                .like(hasKeyword, PsiStockOutOrder::getOrderNo, keyword)
                                .orderByDesc(PsiStockOutOrder::getArchiveTime));
                break;
            case CHECK:
                p = checkOrderMapper.selectPage(new Page<PsiStockCheckOrder>(page, size),
                        new LambdaQueryWrapper<PsiStockCheckOrder>()
                                .eq(PsiStockCheckOrder::getIsArchived, 1)
                                .like(hasKeyword, PsiStockCheckOrder::getCheckNo, keyword)
                                .orderByDesc(PsiStockCheckOrder::getArchiveTime));
                break;
            case TRANSFER:
                p = transferOrderMapper.selectPage(new Page<PsiStockTransferOrder>(page, size),
                        new LambdaQueryWrapper<PsiStockTransferOrder>()
                                .eq(PsiStockTransferOrder::getIsArchived, 1)
                                .like(hasKeyword, PsiStockTransferOrder::getTransferNo, keyword)
                                .orderByDesc(PsiStockTransferOrder::getArchiveTime));
                break;
            default:
                p = returnOrderMapper.selectPage(new Page<PsiReturnOrder>(page, size),
                        new LambdaQueryWrapper<PsiReturnOrder>()
                                .eq(PsiReturnOrder::getIsArchived, 1)
                                .like(hasKeyword, PsiReturnOrder::getReturnNo, keyword)
                                .orderByDesc(PsiReturnOrder::getArchiveTime));
                break;
        }
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    private List<Long> candidateIds(OrderType type, LocalDateTime cutoff) {
        List<Long> ids = new ArrayList<>();
        switch (type) {
            case IN:
                for (PsiStockInOrder o : inOrderMapper.selectList(new LambdaQueryWrapper<PsiStockInOrder>()
                        .in(PsiStockInOrder::getStatus, type.terminalStatuses())
                        .eq(PsiStockInOrder::getIsLocked, 1)
                        .eq(PsiStockInOrder::getIsArchived, 0)
                        .lt(PsiStockInOrder::getCreateTime, cutoff))) {
                    ids.add(o.getId());
                }
                break;
            case OUT:
                for (PsiStockOutOrder o : outOrderMapper.selectList(new LambdaQueryWrapper<PsiStockOutOrder>()
                        .in(PsiStockOutOrder::getStatus, type.terminalStatuses())
                        .eq(PsiStockOutOrder::getIsLocked, 1)
                        .eq(PsiStockOutOrder::getIsArchived, 0)
                        .lt(PsiStockOutOrder::getCreateTime, cutoff))) {
                    ids.add(o.getId());
                }
                break;
            case CHECK:
                for (PsiStockCheckOrder o : checkOrderMapper.selectList(new LambdaQueryWrapper<PsiStockCheckOrder>()
                        .in(PsiStockCheckOrder::getStatus, type.terminalStatuses())
                        .eq(PsiStockCheckOrder::getIsLocked, 1)
                        .eq(PsiStockCheckOrder::getIsArchived, 0)
                        .lt(PsiStockCheckOrder::getCreateTime, cutoff))) {
                    ids.add(o.getId());
                }
                break;
            case TRANSFER:
                for (PsiStockTransferOrder o : transferOrderMapper.selectList(new LambdaQueryWrapper<PsiStockTransferOrder>()
                        .in(PsiStockTransferOrder::getStatus, type.terminalStatuses())
                        .eq(PsiStockTransferOrder::getIsLocked, 1)
                        .eq(PsiStockTransferOrder::getIsArchived, 0)
                        .lt(PsiStockTransferOrder::getCreateTime, cutoff))) {
                    ids.add(o.getId());
                }
                break;
            default:
                for (PsiReturnOrder o : returnOrderMapper.selectList(new LambdaQueryWrapper<PsiReturnOrder>()
                        .in(PsiReturnOrder::getStatus, type.terminalStatuses())
                        .eq(PsiReturnOrder::getIsLocked, 1)
                        .eq(PsiReturnOrder::getIsArchived, 0)
                        .lt(PsiReturnOrder::getCreateTime, cutoff))) {
                    ids.add(o.getId());
                }
                break;
        }
        return ids;
    }

    private long archivedCount(OrderType type) {
        Long c;
        switch (type) {
            case IN:
                c = inOrderMapper.selectCount(new LambdaQueryWrapper<PsiStockInOrder>()
                        .eq(PsiStockInOrder::getIsArchived, 1));
                break;
            case OUT:
                c = outOrderMapper.selectCount(new LambdaQueryWrapper<PsiStockOutOrder>()
                        .eq(PsiStockOutOrder::getIsArchived, 1));
                break;
            case CHECK:
                c = checkOrderMapper.selectCount(new LambdaQueryWrapper<PsiStockCheckOrder>()
                        .eq(PsiStockCheckOrder::getIsArchived, 1));
                break;
            case TRANSFER:
                c = transferOrderMapper.selectCount(new LambdaQueryWrapper<PsiStockTransferOrder>()
                        .eq(PsiStockTransferOrder::getIsArchived, 1));
                break;
            default:
                c = returnOrderMapper.selectCount(new LambdaQueryWrapper<PsiReturnOrder>()
                        .eq(PsiReturnOrder::getIsArchived, 1));
                break;
        }
        return c == null ? 0L : c;
    }

    // ---------------- 查询 ----------------

    public List<PsiOrderRevision> revisions(OrderType type, Long id) {
        return revisionMapper.selectList(new LambdaQueryWrapper<PsiOrderRevision>()
                .eq(PsiOrderRevision::getOrderType, type.name())
                .eq(PsiOrderRevision::getOrderId, id)
                .orderByDesc(PsiOrderRevision::getId));
    }

    public List<PsiOrderActionLog> actions(OrderType type, Long id) {
        return actionLogMapper.selectList(new LambdaQueryWrapper<PsiOrderActionLog>()
                .eq(PsiOrderActionLog::getOrderType, type.name())
                .eq(PsiOrderActionLog::getOrderId, id)
                .orderByAsc(PsiOrderActionLog::getId));
    }

    public PageResult<PsiOrderRevision> pendingRevisions(long page, long size) {
        IPage<PsiOrderRevision> p = revisionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PsiOrderRevision>()
                        .eq(PsiOrderRevision::getStatus, "PENDING")
                        .orderByAsc(PsiOrderRevision::getId));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    // ---------------- 供 5 个业务 Service 复用的守卫 ----------------

    /** 锁定即拒绝（编辑/删除/作废前置校验） */
    public void assertNotLocked(Integer isLocked) {
        if (isLocked != null && isLocked == 1) {
            throw BizException.status(LOCKED_MSG);
        }
    }

    /** 执行/作废成功后补齐锁定字段（调用方随后 updateById 落库） */
    public void markLocked(LockAware order) {
        order.setIsLocked(1);
        order.setConfirmUserId(operator.id());
        order.setConfirmUserName(operator.name());
        order.setConfirmTime(LocalDateTime.now());
    }

    /** 写统一操作流水 */
    public void recordAction(OrderType type, Long orderId, String orderNo, String action, String remark) {
        PsiOrderActionLog log = new PsiOrderActionLog();
        log.setOrderType(type.name());
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setAction(action);
        log.setOperatorId(operator.id());
        log.setOperatorName(operator.name());
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        actionLogMapper.insert(log);
    }

    // ---------------- 内部 ----------------

    private PsiOrderRevision requirePending(Long revisionId) {
        PsiOrderRevision rev = revisionMapper.selectById(revisionId);
        if (rev == null) {
            throw BizException.notFound("修订申请不存在");
        }
        if (!"PENDING".equals(rev.getStatus())) {
            throw BizException.status("该修订申请已处理，状态：" + rev.getStatus());
        }
        return rev;
    }

    private boolean isLocked(LockAware order) {
        return order.getIsLocked() != null && order.getIsLocked() == 1;
    }

    private boolean isArchived(LockAware order) {
        return order.getIsArchived() != null && order.getIsArchived() == 1;
    }

    private LockAware load(OrderType type, Long id) {
        LockAware order = null;
        switch (type) {
            case IN:
                order = inOrderMapper.selectById(id);
                break;
            case OUT:
                order = outOrderMapper.selectById(id);
                break;
            case CHECK:
                order = checkOrderMapper.selectById(id);
                break;
            case TRANSFER:
                order = transferOrderMapper.selectById(id);
                break;
            case RETURN:
                order = returnOrderMapper.selectById(id);
                break;
        }
        if (order == null) {
            throw BizException.notFound("单据不存在");
        }
        return order;
    }

    private void save(OrderType type, LockAware order) {
        switch (type) {
            case IN:
                inOrderMapper.updateById((PsiStockInOrder) order);
                break;
            case OUT:
                outOrderMapper.updateById((PsiStockOutOrder) order);
                break;
            case CHECK:
                checkOrderMapper.updateById((PsiStockCheckOrder) order);
                break;
            case TRANSFER:
                transferOrderMapper.updateById((PsiStockTransferOrder) order);
                break;
            case RETURN:
                returnOrderMapper.updateById((PsiReturnOrder) order);
                break;
        }
    }

    /** 解锁时必须显式把 confirm_* 置空，MyBatis-Plus 默认不写 null 字段，故用 update wrapper */
    private void unlock(OrderType type, Long id) {
        switch (type) {
            case IN:
                inOrderMapper.update(null, new LambdaUpdateWrapper<PsiStockInOrder>()
                        .eq(PsiStockInOrder::getId, id)
                        .set(PsiStockInOrder::getIsLocked, 0)
                        .set(PsiStockInOrder::getConfirmUserId, null)
                        .set(PsiStockInOrder::getConfirmUserName, null)
                        .set(PsiStockInOrder::getConfirmTime, null));
                break;
            case OUT:
                outOrderMapper.update(null, new LambdaUpdateWrapper<PsiStockOutOrder>()
                        .eq(PsiStockOutOrder::getId, id)
                        .set(PsiStockOutOrder::getIsLocked, 0)
                        .set(PsiStockOutOrder::getConfirmUserId, null)
                        .set(PsiStockOutOrder::getConfirmUserName, null)
                        .set(PsiStockOutOrder::getConfirmTime, null));
                break;
            case CHECK:
                checkOrderMapper.update(null, new LambdaUpdateWrapper<PsiStockCheckOrder>()
                        .eq(PsiStockCheckOrder::getId, id)
                        .set(PsiStockCheckOrder::getIsLocked, 0)
                        .set(PsiStockCheckOrder::getConfirmUserId, null)
                        .set(PsiStockCheckOrder::getConfirmUserName, null)
                        .set(PsiStockCheckOrder::getConfirmTime, null));
                break;
            case TRANSFER:
                transferOrderMapper.update(null, new LambdaUpdateWrapper<PsiStockTransferOrder>()
                        .eq(PsiStockTransferOrder::getId, id)
                        .set(PsiStockTransferOrder::getIsLocked, 0)
                        .set(PsiStockTransferOrder::getConfirmUserId, null)
                        .set(PsiStockTransferOrder::getConfirmUserName, null)
                        .set(PsiStockTransferOrder::getConfirmTime, null));
                break;
            case RETURN:
                returnOrderMapper.update(null, new LambdaUpdateWrapper<PsiReturnOrder>()
                        .eq(PsiReturnOrder::getId, id)
                        .set(PsiReturnOrder::getIsLocked, 0)
                        .set(PsiReturnOrder::getConfirmUserId, null)
                        .set(PsiReturnOrder::getConfirmUserName, null)
                        .set(PsiReturnOrder::getConfirmTime, null));
                break;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
