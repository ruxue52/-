package com.shuhaige.psi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.OrderNoUtil;
import com.shuhaige.psi.common.OrderType;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.dto.CheckOrderDTO;
import com.shuhaige.psi.dto.ReturnOrderDTO;
import com.shuhaige.psi.dto.TransferOrderDTO;
import com.shuhaige.psi.entity.*;
import com.shuhaige.psi.mapper.*;
import com.shuhaige.psi.vo.StockFlowVO;
import com.shuhaige.psi.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final PsiStockMapper stockMapper;
    private final PsiStockFlowMapper flowMapper;
    private final PsiStockCheckOrderMapper checkOrderMapper;
    private final PsiStockCheckItemMapper checkItemMapper;
    private final PsiStockTransferOrderMapper transferOrderMapper;
    private final PsiStockTransferItemMapper transferItemMapper;
    private final PsiReturnOrderMapper returnOrderMapper;
    private final PsiReturnItemMapper returnItemMapper;
    private final BaseBookMapper bookMapper;
    private final BaseSupplierMapper supplierMapper;
    private final BaseCustomerMapper customerMapper;
    private final CurrentOperator operator;
    private final OrderDocService orderDocService;

    // ---------------- 库存查询 ----------------

    public PageResult<StockVO> stockList(long page, long size, Long warehouseId, Long categoryId,
                                         String keyword, boolean onlyLow) {
        IPage<StockVO> p = stockMapper.pageStock(new Page<>(page, size), warehouseId, categoryId, keyword, onlyLow);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public List<StockVO> stockByBook(Long bookId) {
        return stockMapper.listByBook(bookId);
    }

    public List<StockVO> warnings() {
        return stockMapper.listWarnings();
    }

    public Map<String, Object> summary() {
        return stockMapper.summary();
    }

    public PageResult<StockFlowVO> flows(long page, long size, Long bookId, Long warehouseId,
                                         String bizType, String startDate, String endDate) {
        IPage<StockFlowVO> p = flowMapper.pageFlows(new Page<>(page, size), bookId, warehouseId, bizType, startDate, endDate);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    // ---------------- 盘点 ----------------

    @Transactional
    public Long createCheck(CheckOrderDTO dto) {
        if (dto.getWarehouseId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库不能为空");
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String checkNo = OrderNoUtil.next("CK", checkOrderMapper.maxCheckNo("CK" + today));
        PsiStockCheckOrder order = new PsiStockCheckOrder();
        order.setCheckNo(checkNo);
        order.setWarehouseId(dto.getWarehouseId());
        order.setCheckType(dto.getCheckType() == null ? "FULL" : dto.getCheckType());
        order.setStatus("DRAFT");
        order.setRemark(dto.getRemark());
        order.setOperatorId(operator.id());
        order.setOperatorName(operator.name());
        order.setCreateTime(LocalDateTime.now());
        order.setTotalItems(0);
        order.setDiffItems(0);
        order.setProfitQty(BigDecimal.ZERO);
        order.setLossQty(BigDecimal.ZERO);
        checkOrderMapper.insert(order);

        LambdaQueryWrapper<PsiStock> qw = new LambdaQueryWrapper<PsiStock>()
                .eq(PsiStock::getWarehouseId, dto.getWarehouseId());
        if (dto.getBookIds() != null && !dto.getBookIds().isEmpty()) {
            qw.in(PsiStock::getBookId, dto.getBookIds());
        }
        List<PsiStock> stocks = stockMapper.selectList(qw);
        for (PsiStock s : stocks) {
            PsiStockCheckItem item = new PsiStockCheckItem();
            item.setOrderId(order.getId());
            item.setBookId(s.getBookId());
            BaseBook book = bookMapper.selectById(s.getBookId());
            item.setBookName(book == null ? null : book.getBookName());
            item.setIsbn(book == null ? null : book.getIsbn());
            item.setBookQty(s.getQuantity());
            item.setRealQty(s.getQuantity());
            item.setDiffQty(BigDecimal.ZERO);
            item.setDiffAmount(BigDecimal.ZERO);
            checkItemMapper.insert(item);
        }
        order.setTotalItems(stocks.size());
        checkOrderMapper.updateById(order);
        return order.getId();
    }

    public PageResult<PsiStockCheckOrder> checkPage(long page, long size, String status, Long warehouseId) {
        LambdaQueryWrapper<PsiStockCheckOrder> qw = new LambdaQueryWrapper<>();
        // 已归档单据不在常规列表展示（归档页单独查询）
        qw.eq(PsiStockCheckOrder::getIsArchived, 0);
        if (status != null && !status.isEmpty()) {
            qw.eq(PsiStockCheckOrder::getStatus, status);
        }
        if (warehouseId != null) {
            qw.eq(PsiStockCheckOrder::getWarehouseId, warehouseId);
        }
        qw.orderByDesc(PsiStockCheckOrder::getId);
        IPage<PsiStockCheckOrder> p = checkOrderMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public Map<String, Object> checkDetail(Long id) {
        PsiStockCheckOrder order = checkOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("盘点单不存在");
        }
        List<PsiStockCheckItem> items = checkItemMapper.selectList(new LambdaQueryWrapper<PsiStockCheckItem>()
                .eq(PsiStockCheckItem::getOrderId, id).orderByAsc(PsiStockCheckItem::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return data;
    }

    @Transactional
    public void updateCheckItems(Long id, List<CheckOrderDTO.ItemUpdate> updates) {
        PsiStockCheckOrder order = checkOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("盘点单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅盘点中状态可录入");
        }
        for (CheckOrderDTO.ItemUpdate up : updates) {
            PsiStockCheckItem item = checkItemMapper.selectById(up.getItemId());
            if (item == null) {
                continue;
            }
            BigDecimal realQty = up.getRealQty() == null ? BigDecimal.ZERO : up.getRealQty();
            BigDecimal diff = realQty.subtract(item.getBookQty());
            item.setRealQty(realQty);
            item.setDiffQty(diff);
            PsiStock stock = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, order.getWarehouseId()));
            BigDecimal cost = stock == null ? BigDecimal.ZERO : stock.getAvgCost();
            item.setDiffAmount(diff.multiply(cost).setScale(2, RoundingMode.HALF_UP));
            checkItemMapper.updateById(item);
        }
    }

    /** 完成盘点：按差异调整库存、写流水 */
    @Transactional
    public void finishCheck(Long id) {
        PsiStockCheckOrder order = checkOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("盘点单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅盘点中状态可完成");
        }
        List<PsiStockCheckItem> items = checkItemMapper.selectList(new LambdaQueryWrapper<PsiStockCheckItem>()
                .eq(PsiStockCheckItem::getOrderId, id));
        LocalDateTime now = LocalDateTime.now();
        BigDecimal profitQty = BigDecimal.ZERO;
        BigDecimal lossQty = BigDecimal.ZERO;
        int diffItems = 0;
        for (PsiStockCheckItem item : items) {
            BigDecimal diff = item.getRealQty().subtract(item.getBookQty());
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            diffItems++;
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                profitQty = profitQty.add(diff);
            } else {
                lossQty = lossQty.add(diff.abs());
            }
            PsiStock stock = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, order.getWarehouseId()));
            BigDecimal before = stock == null ? BigDecimal.ZERO : stock.getQuantity();
            BigDecimal cost = stock == null ? BigDecimal.ZERO : stock.getAvgCost();
            BigDecimal after = before.add(diff);
            if (stock == null) {
                stock = new PsiStock();
                stock.setBookId(item.getBookId());
                stock.setWarehouseId(order.getWarehouseId());
                stock.setQuantity(after);
                stock.setLockedQty(BigDecimal.ZERO);
                stock.setSafetyStock(BigDecimal.ZERO);
                stock.setAvgCost(cost);
                stock.setUpdateTime(now);
                stockMapper.insert(stock);
            } else {
                stock.setQuantity(after);
                stock.setUpdateTime(now);
                stockMapper.updateById(stock);
            }
            PsiStockFlow flow = new PsiStockFlow();
            flow.setBizType("CHECK");
            flow.setBizNo(order.getCheckNo());
            flow.setBookId(item.getBookId());
            flow.setWarehouseId(order.getWarehouseId());
            flow.setDirection(diff.compareTo(BigDecimal.ZERO) > 0 ? 1 : -1);
            flow.setQuantity(diff.abs());
            flow.setBeforeQty(before);
            flow.setAfterQty(after);
            flow.setUnitCost(cost);
            flow.setOperatorId(operator.id());
            flow.setOperatorName(operator.name());
            flow.setSource("PC");
            flow.setCreateTime(now);
            flowMapper.insert(flow);
        }
        order.setProfitQty(profitQty);
        order.setLossQty(lossQty);
        order.setDiffItems(diffItems);
        order.setStatus("FINISHED");
        order.setFinishTime(now);
        orderDocService.markLocked(order);
        checkOrderMapper.updateById(order);
        orderDocService.recordAction(OrderType.CHECK, id, order.getCheckNo(), "FINISH", "完成盘点");
    }

    @Transactional
    public void cancelCheck(Long id) {
        PsiStockCheckOrder order = checkOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("盘点单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅盘点中状态可作废");
        }
        order.setStatus("CANCELLED");
        orderDocService.markLocked(order);
        checkOrderMapper.updateById(order);
        orderDocService.recordAction(OrderType.CHECK, id, order.getCheckNo(), "CANCEL", "作废盘点单");
    }

    // ---------------- 调拨 ----------------

    @Transactional
    public Long createTransfer(TransferOrderDTO dto) {
        if (dto.getFromWarehouse() == null || dto.getToWarehouse() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "调出/调入仓库不能为空");
        }
        if (dto.getFromWarehouse().equals(dto.getToWarehouse())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "调出与调入仓库不能相同");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "调拨明细不能为空");
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transferNo = OrderNoUtil.next("TR", transferOrderMapper.maxTransferNo("TR" + today));
        PsiStockTransferOrder order = new PsiStockTransferOrder();
        order.setTransferNo(transferNo);
        order.setFromWarehouse(dto.getFromWarehouse());
        order.setToWarehouse(dto.getToWarehouse());
        order.setStatus("DRAFT");
        order.setRemark(dto.getRemark());
        order.setOperatorId(operator.id());
        order.setOperatorName(operator.name());
        order.setCreateTime(LocalDateTime.now());
        BigDecimal totalQty = BigDecimal.ZERO;
        for (TransferOrderDTO.Item it : dto.getItems()) {
            totalQty = totalQty.add(it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity());
        }
        order.setTotalQty(totalQty);
        transferOrderMapper.insert(order);
        for (TransferOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            PsiStockTransferItem item = new PsiStockTransferItem();
            item.setOrderId(order.getId());
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(it.getQuantity());
            transferItemMapper.insert(item);
        }
        return order.getId();
    }

    @Transactional
    public void executeTransfer(Long id) {
        PsiStockTransferOrder order = transferOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("调拨单不存在");
        }
        if (!"DRAFT".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
            throw BizException.status("当前状态不可执行调拨");
        }
        List<PsiStockTransferItem> items = transferItemMapper.selectList(new LambdaQueryWrapper<PsiStockTransferItem>()
                .eq(PsiStockTransferItem::getOrderId, id));
        LocalDateTime now = LocalDateTime.now();
        for (PsiStockTransferItem item : items) {
            PsiStock from = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, order.getFromWarehouse()));
            if (from == null || from.getQuantity().compareTo(item.getQuantity()) < 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "调出仓库存不足: " + item.getBookName());
            }
            BigDecimal cost = from.getAvgCost();
            BigDecimal outBefore = from.getQuantity();
            BigDecimal outAfter = outBefore.subtract(item.getQuantity());
            from.setQuantity(outAfter);
            from.setLastOutTime(now);
            from.setUpdateTime(now);
            stockMapper.updateById(from);
            insertFlow("TRANSFER_OUT", order.getTransferNo(), item.getBookId(), order.getFromWarehouse(),
                    -1, item.getQuantity(), outBefore, outAfter, cost, now);

            PsiStock to = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, order.getToWarehouse()));
            BigDecimal inBefore = to == null ? BigDecimal.ZERO : to.getQuantity();
            BigDecimal inAfter = inBefore.add(item.getQuantity());
            if (to == null) {
                to = new PsiStock();
                to.setBookId(item.getBookId());
                to.setWarehouseId(order.getToWarehouse());
                to.setQuantity(inAfter);
                to.setLockedQty(BigDecimal.ZERO);
                to.setSafetyStock(BigDecimal.ZERO);
                to.setAvgCost(cost);
                to.setLastInTime(now);
                to.setUpdateTime(now);
                stockMapper.insert(to);
            } else {
                BigDecimal newCost = inAfter.compareTo(BigDecimal.ZERO) > 0
                        ? inBefore.multiply(to.getAvgCost()).add(item.getQuantity().multiply(cost))
                                .divide(inAfter, 4, RoundingMode.HALF_UP)
                        : cost;
                to.setQuantity(inAfter);
                to.setAvgCost(newCost);
                to.setLastInTime(now);
                to.setUpdateTime(now);
                stockMapper.updateById(to);
            }
            insertFlow("TRANSFER_IN", order.getTransferNo(), item.getBookId(), order.getToWarehouse(),
                    1, item.getQuantity(), inBefore, inAfter, cost, now);
        }
        order.setStatus("TRANSFERRED");
        order.setTransferTime(now);
        orderDocService.markLocked(order);
        transferOrderMapper.updateById(order);
        orderDocService.recordAction(OrderType.TRANSFER, id, order.getTransferNo(), "TRANSFER", "执行调拨");
    }

    /** 调拨单分页列表 */
    public PageResult<PsiStockTransferOrder> transferPage(long page, long size, String status, String keyword) {
        LambdaQueryWrapper<PsiStockTransferOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(PsiStockTransferOrder::getIsArchived, 0);
        if (status != null && !status.isEmpty()) {
            qw.eq(PsiStockTransferOrder::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(PsiStockTransferOrder::getTransferNo, keyword);
        }
        qw.orderByDesc(PsiStockTransferOrder::getId);
        IPage<PsiStockTransferOrder> p = transferOrderMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    /** 调拨单详情（含明细） */
    public Map<String, Object> transferDetail(Long id) {
        PsiStockTransferOrder order = transferOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("调拨单不存在");
        }
        List<PsiStockTransferItem> items = transferItemMapper.selectList(new LambdaQueryWrapper<PsiStockTransferItem>()
                .eq(PsiStockTransferItem::getOrderId, id).orderByAsc(PsiStockTransferItem::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return data;
    }

    /** 编辑调拨单：仅草稿且未锁定 */
    @Transactional
    public void updateTransfer(Long id, TransferOrderDTO dto) {
        PsiStockTransferOrder order = transferOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("调拨单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可修改");
        }
        if (dto.getFromWarehouse() == null || dto.getToWarehouse() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "调出/调入仓库不能为空");
        }
        if (dto.getFromWarehouse().equals(dto.getToWarehouse())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "调出与调入仓库不能相同");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "调拨明细不能为空");
        }
        transferItemMapper.delete(new LambdaQueryWrapper<PsiStockTransferItem>()
                .eq(PsiStockTransferItem::getOrderId, id));
        BigDecimal totalQty = BigDecimal.ZERO;
        for (TransferOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            BigDecimal qty = it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity();
            PsiStockTransferItem item = new PsiStockTransferItem();
            item.setOrderId(id);
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(qty);
            transferItemMapper.insert(item);
            totalQty = totalQty.add(qty);
        }
        order.setFromWarehouse(dto.getFromWarehouse());
        order.setToWarehouse(dto.getToWarehouse());
        order.setRemark(dto.getRemark());
        order.setTotalQty(totalQty);
        transferOrderMapper.updateById(order);
    }

    // ---------------- 退货 ----------------

    @Transactional
    public Long createReturn(ReturnOrderDTO dto) {
        if (dto.getReturnType() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "退货类型不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "退货明细不能为空");
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String returnNo = OrderNoUtil.next("RT", returnOrderMapper.maxReturnNo("RT" + today));
        PsiReturnOrder order = new PsiReturnOrder();
        order.setReturnNo(returnNo);
        order.setReturnType(dto.getReturnType());
        order.setPartnerId(dto.getPartnerId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setReason(dto.getReason());
        order.setStatus("DRAFT");
        order.setOperatorId(operator.id());
        order.setOperatorName(operator.name());
        order.setCreateTime(LocalDateTime.now());
        if ("PURCHASE".equals(dto.getReturnType()) && dto.getPartnerId() != null) {
            BaseSupplier s = supplierMapper.selectById(dto.getPartnerId());
            order.setPartnerName(s == null ? null : s.getSupplierName());
        } else if ("SALES".equals(dto.getReturnType()) && dto.getPartnerId() != null) {
            BaseCustomer c = customerMapper.selectById(dto.getPartnerId());
            order.setPartnerName(c == null ? null : c.getCustomerName());
        }
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PsiReturnItem> items = new ArrayList<>();
        for (ReturnOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            BigDecimal qty = it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity();
            BigDecimal price = it.getPrice() == null ? BigDecimal.ZERO : it.getPrice();
            PsiReturnItem item = new PsiReturnItem();
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(qty);
            item.setPrice(price);
            item.setAmount(qty.multiply(price));
            items.add(item);
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        returnOrderMapper.insert(order);
        for (PsiReturnItem item : items) {
            item.setOrderId(order.getId());
            returnItemMapper.insert(item);
        }
        return order.getId();
    }

    @Transactional
    public void finishReturn(Long id) {
        PsiReturnOrder order = returnOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("退货单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可完成退货");
        }
        boolean purchase = "PURCHASE".equals(order.getReturnType());
        List<PsiReturnItem> items = returnItemMapper.selectList(new LambdaQueryWrapper<PsiReturnItem>()
                .eq(PsiReturnItem::getOrderId, id));
        LocalDateTime now = LocalDateTime.now();
        for (PsiReturnItem item : items) {
            PsiStock stock = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, order.getWarehouseId()));
            BigDecimal before = stock == null ? BigDecimal.ZERO : stock.getQuantity();
            BigDecimal cost = stock == null ? BigDecimal.ZERO : stock.getAvgCost();
            // 采购退货 -> 库存减少(流出)；销售退货 -> 库存增加(流入)
            BigDecimal after = purchase ? before.subtract(item.getQuantity()) : before.add(item.getQuantity());
            if (stock == null) {
                stock = new PsiStock();
                stock.setBookId(item.getBookId());
                stock.setWarehouseId(order.getWarehouseId());
                stock.setQuantity(after);
                stock.setLockedQty(BigDecimal.ZERO);
                stock.setSafetyStock(BigDecimal.ZERO);
                stock.setAvgCost(cost);
                stock.setUpdateTime(now);
                stockMapper.insert(stock);
            } else {
                stock.setQuantity(after);
                stock.setUpdateTime(now);
                stockMapper.updateById(stock);
            }
            PsiStockFlow flow = new PsiStockFlow();
            flow.setBizType(purchase ? "RET_PUR" : "RET_SAL");
            flow.setBizNo(order.getReturnNo());
            flow.setBookId(item.getBookId());
            flow.setWarehouseId(order.getWarehouseId());
            flow.setDirection(purchase ? -1 : 1);
            flow.setQuantity(item.getQuantity());
            flow.setBeforeQty(before);
            flow.setAfterQty(after);
            flow.setUnitCost(cost);
            flow.setOperatorId(operator.id());
            flow.setOperatorName(operator.name());
            flow.setSource("PC");
            flow.setCreateTime(now);
            flowMapper.insert(flow);
        }
        order.setStatus("FINISHED");
        order.setFinishTime(now);
        orderDocService.markLocked(order);
        returnOrderMapper.updateById(order);
        orderDocService.recordAction(OrderType.RETURN, id, order.getReturnNo(), "FINISH", "完成退货");
    }

    /** 退货单分页列表 */
    public PageResult<PsiReturnOrder> returnPage(long page, long size, String status, String returnType, String keyword) {
        LambdaQueryWrapper<PsiReturnOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(PsiReturnOrder::getIsArchived, 0);
        if (status != null && !status.isEmpty()) {
            qw.eq(PsiReturnOrder::getStatus, status);
        }
        if (returnType != null && !returnType.isEmpty()) {
            qw.eq(PsiReturnOrder::getReturnType, returnType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(PsiReturnOrder::getReturnNo, keyword);
        }
        qw.orderByDesc(PsiReturnOrder::getId);
        IPage<PsiReturnOrder> p = returnOrderMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    /** 退货单详情（含明细） */
    public Map<String, Object> returnDetail(Long id) {
        PsiReturnOrder order = returnOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("退货单不存在");
        }
        List<PsiReturnItem> items = returnItemMapper.selectList(new LambdaQueryWrapper<PsiReturnItem>()
                .eq(PsiReturnItem::getOrderId, id).orderByAsc(PsiReturnItem::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return data;
    }

    /** 编辑退货单：仅草稿且未锁定 */
    @Transactional
    public void updateReturn(Long id, ReturnOrderDTO dto) {
        PsiReturnOrder order = returnOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("退货单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可修改");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "退货明细不能为空");
        }
        returnItemMapper.delete(new LambdaQueryWrapper<PsiReturnItem>()
                .eq(PsiReturnItem::getOrderId, id));
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ReturnOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            BigDecimal qty = it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity();
            BigDecimal price = it.getPrice() == null ? BigDecimal.ZERO : it.getPrice();
            BigDecimal amount = qty.multiply(price);
            PsiReturnItem item = new PsiReturnItem();
            item.setOrderId(id);
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(qty);
            item.setPrice(price);
            item.setAmount(amount);
            returnItemMapper.insert(item);
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(amount);
        }
        if (dto.getReturnType() != null) {
            order.setReturnType(dto.getReturnType());
        }
        order.setPartnerId(dto.getPartnerId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setReason(dto.getReason());
        if ("PURCHASE".equals(order.getReturnType()) && dto.getPartnerId() != null) {
            BaseSupplier s = supplierMapper.selectById(dto.getPartnerId());
            order.setPartnerName(s == null ? null : s.getSupplierName());
        } else if ("SALES".equals(order.getReturnType()) && dto.getPartnerId() != null) {
            BaseCustomer c = customerMapper.selectById(dto.getPartnerId());
            order.setPartnerName(c == null ? null : c.getCustomerName());
        } else {
            order.setPartnerName(null);
        }
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        returnOrderMapper.updateById(order);
    }

    private void insertFlow(String bizType, String bizNo, Long bookId, Long warehouseId, int direction,
                            BigDecimal qty, BigDecimal before, BigDecimal after, BigDecimal cost, LocalDateTime now) {
        PsiStockFlow flow = new PsiStockFlow();
        flow.setBizType(bizType);
        flow.setBizNo(bizNo);
        flow.setBookId(bookId);
        flow.setWarehouseId(warehouseId);
        flow.setDirection(direction);
        flow.setQuantity(qty);
        flow.setBeforeQty(before);
        flow.setAfterQty(after);
        flow.setUnitCost(cost);
        flow.setOperatorId(operator.id());
        flow.setOperatorName(operator.name());
        flow.setSource("PC");
        flow.setCreateTime(now);
        flowMapper.insert(flow);
    }
}