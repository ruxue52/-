package com.shuhaige.psi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.OrderNoUtil;
import com.shuhaige.psi.common.OrderType;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.dto.PurchaseOrderDTO;
import com.shuhaige.psi.entity.*;
import com.shuhaige.psi.mapper.*;
import com.shuhaige.psi.vo.PurchaseOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PsiStockInOrderMapper orderMapper;
    private final PsiStockInItemMapper itemMapper;
    private final PsiStockInLogMapper logMapper;
    private final PsiStockMapper stockMapper;
    private final PsiStockFlowMapper flowMapper;
    private final BaseBookMapper bookMapper;
    private final CurrentOperator operator;
    private final OrderDocService orderDocService;

    public PageResult<PurchaseOrderVO> page(long page, long size, String status, Long supplierId,
                                            Long warehouseId, String keyword, String startDate, String endDate) {
        IPage<PurchaseOrderVO> p = orderMapper.pageOrders(new Page<>(page, size), status, supplierId,
                warehouseId, keyword, startDate, endDate);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public PurchaseOrderVO detail(Long id) {
        PurchaseOrderVO vo = orderMapper.selectDetail(id);
        if (vo == null) {
            throw BizException.notFound("进货单不存在");
        }
        vo.setItems(itemMapper.selectList(new LambdaQueryWrapper<PsiStockInItem>()
                .eq(PsiStockInItem::getOrderId, id).orderByAsc(PsiStockInItem::getId)));
        return vo;
    }

    public List<PsiStockInLog> logs(Long id) {
        return logMapper.selectList(new LambdaQueryWrapper<PsiStockInLog>()
                .eq(PsiStockInLog::getOrderId, id).orderByAsc(PsiStockInLog::getId));
    }

    @Transactional
    public Long create(PurchaseOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "进货明细不能为空");
        }
        String orderNo = OrderNoUtil.next("PI", orderMapper.maxOrderNo("PI" + java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))));
        PsiStockInOrder order = new PsiStockInOrder();
        order.setOrderNo(orderNo);
        order.setSupplierId(dto.getSupplierId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setSettleType(dto.getSettleType() == null ? "CASH" : dto.getSettleType());
        order.setDiscount(nvl(dto.getDiscount()));
        order.setPaidAmount(nvl(dto.getPaidAmount()));
        order.setRemark(dto.getRemark());
        order.setSource(dto.getSource() == null ? "PC" : dto.getSource());
        order.setStatus("DRAFT");
        order.setOperatorId(operator.id());
        order.setOperatorName(operator.name());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PsiStockInItem> items = new ArrayList<>();
        for (PurchaseOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            BigDecimal qty = nvl(it.getQuantity());
            BigDecimal price = nvl(it.getPrice());
            BigDecimal amount = qty.multiply(price);
            PsiStockInItem item = new PsiStockInItem();
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(qty);
            item.setPrice(price);
            item.setAmount(amount);
            item.setBatchNo(it.getBatchNo());
            item.setRemark(it.getRemark());
            items.add(item);
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(amount);
        }
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount.subtract(order.getDiscount()));
        orderMapper.insert(order);

        for (PsiStockInItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }
        writeLog(order.getId(), "CREATE", "创建进货单");
        return order.getId();
    }

    @Transactional
    public void update(Long id, PurchaseOrderDTO dto) {
        PsiStockInOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("进货单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可修改");
        }
        itemMapper.delete(new LambdaQueryWrapper<PsiStockInItem>().eq(PsiStockInItem::getOrderId, id));
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            BigDecimal qty = nvl(it.getQuantity());
            BigDecimal price = nvl(it.getPrice());
            BigDecimal amount = qty.multiply(price);
            PsiStockInItem item = new PsiStockInItem();
            item.setOrderId(id);
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(qty);
            item.setPrice(price);
            item.setAmount(amount);
            item.setBatchNo(it.getBatchNo());
            item.setRemark(it.getRemark());
            itemMapper.insert(item);
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(amount);
        }
        order.setSupplierId(dto.getSupplierId());
        order.setWarehouseId(dto.getWarehouseId());
        if (dto.getSettleType() != null) {
            order.setSettleType(dto.getSettleType());
        }
        order.setDiscount(nvl(dto.getDiscount()));
        order.setPaidAmount(nvl(dto.getPaidAmount()));
        order.setRemark(dto.getRemark());
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount.subtract(order.getDiscount()));
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void delete(Long id) {
        PsiStockInOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("进货单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可删除");
        }
        itemMapper.delete(new LambdaQueryWrapper<PsiStockInItem>().eq(PsiStockInItem::getOrderId, id));
        logMapper.delete(new LambdaQueryWrapper<PsiStockInLog>().eq(PsiStockInLog::getOrderId, id));
        orderMapper.deleteById(id);
    }

    @Transactional
    public void submit(Long id) {
        PsiStockInOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("进货单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可提交");
        }
        order.setStatus("SUBMITTED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        writeLog(id, "SUBMIT", "提交进货单");
    }

    @Transactional
    public void cancel(Long id) {
        PsiStockInOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("进货单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if ("INSTOCK".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw BizException.status("当前状态不可作废");
        }
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderDocService.markLocked(order);
        orderMapper.updateById(order);
        writeLog(id, "CANCEL", "作废进货单");
    }

    /** 确认入库：写入库存与流水，状态 -> INSTOCK */
    @Transactional
    public void instock(Long id) {
        PsiStockInOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("进货单不存在");
        }
        if (!"DRAFT".equals(order.getStatus()) && !"SUBMITTED".equals(order.getStatus())) {
            throw BizException.status("仅草稿/已提交状态可入库");
        }
        List<PsiStockInItem> items = itemMapper.selectList(new LambdaQueryWrapper<PsiStockInItem>()
                .eq(PsiStockInItem::getOrderId, id));
        Long whId = order.getWarehouseId();
        LocalDateTime now = LocalDateTime.now();
        for (PsiStockInItem item : items) {
            PsiStock stock = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, whId));
            BigDecimal before = stock == null ? BigDecimal.ZERO : stock.getQuantity();
            BigDecimal oldCost = stock == null ? BigDecimal.ZERO : stock.getAvgCost();
            BigDecimal qty = item.getQuantity();
            BigDecimal price = item.getPrice();
            BigDecimal after = before.add(qty);
            BigDecimal newCost;
            if (after.compareTo(BigDecimal.ZERO) > 0) {
                newCost = before.multiply(oldCost).add(qty.multiply(price))
                        .divide(after, 4, RoundingMode.HALF_UP);
            } else {
                newCost = price;
            }
            if (stock == null) {
                stock = new PsiStock();
                stock.setBookId(item.getBookId());
                stock.setWarehouseId(whId);
                stock.setQuantity(after);
                stock.setLockedQty(BigDecimal.ZERO);
                stock.setSafetyStock(BigDecimal.ZERO);
                stock.setAvgCost(newCost);
                stock.setLastInTime(now);
                stock.setUpdateTime(now);
                stockMapper.insert(stock);
            } else {
                stock.setQuantity(after);
                stock.setAvgCost(newCost);
                stock.setLastInTime(now);
                stock.setUpdateTime(now);
                stockMapper.updateById(stock);
            }
            PsiStockFlow flow = new PsiStockFlow();
            flow.setBizType("IN");
            flow.setBizNo(order.getOrderNo());
            flow.setBookId(item.getBookId());
            flow.setWarehouseId(whId);
            flow.setDirection(1);
            flow.setQuantity(qty);
            flow.setBeforeQty(before);
            flow.setAfterQty(after);
            flow.setUnitCost(price);
            flow.setOperatorId(operator.id());
            flow.setOperatorName(operator.name());
            flow.setSource(order.getSource());
            flow.setCreateTime(now);
            flowMapper.insert(flow);
        }
        order.setStatus("INSTOCK");
        order.setInTime(now);
        order.setUpdateTime(now);
        orderDocService.markLocked(order);
        orderMapper.updateById(order);
        writeLog(id, "INSTOCK", "确认入库");
    }

    /** 统一写入 psi_order_action_log（原 psi_stock_in_log 不再新增写入） */
    private void writeLog(Long orderId, String action, String remark) {
        PsiStockInOrder order = orderMapper.selectById(orderId);
        String orderNo = order == null ? null : order.getOrderNo();
        orderDocService.recordAction(OrderType.IN, orderId, orderNo, action, remark);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}