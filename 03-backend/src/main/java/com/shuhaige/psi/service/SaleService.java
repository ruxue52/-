package com.shuhaige.psi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.OrderNoUtil;
import com.shuhaige.psi.common.OrderType;
import com.shuhaige.psi.common.PageResult;
import com.shuhaige.psi.dto.SaleOrderDTO;
import com.shuhaige.psi.entity.*;
import com.shuhaige.psi.mapper.*;
import com.shuhaige.psi.vo.SaleOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final PsiStockOutOrderMapper orderMapper;
    private final PsiStockOutItemMapper itemMapper;
    private final PsiStockMapper stockMapper;
    private final PsiStockFlowMapper flowMapper;
    private final BaseBookMapper bookMapper;
    private final BaseCustomerMapper customerMapper;
    private final BaseMemberLevelMapper levelMapper;
    private final CurrentOperator operator;
    private final OrderDocService orderDocService;

    public PageResult<SaleOrderVO> page(long page, long size, String status, Long customerId,
                                        Long warehouseId, String keyword, String startDate, String endDate) {
        IPage<SaleOrderVO> p = orderMapper.pageOrders(new Page<>(page, size), status, customerId,
                warehouseId, keyword, startDate, endDate);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public SaleOrderVO detail(Long id) {
        SaleOrderVO vo = orderMapper.selectDetail(id);
        if (vo == null) {
            throw BizException.notFound("出货单不存在");
        }
        vo.setItems(itemMapper.selectList(new LambdaQueryWrapper<PsiStockOutItem>()
                .eq(PsiStockOutItem::getOrderId, id).orderByAsc(PsiStockOutItem::getId)));
        return vo;
    }

    public List<Map<String, Object>> posSearch(String keyword, Long warehouseId) {
        if (keyword == null || keyword.isEmpty()) {
            return List.of();
        }
        return stockMapper.posSearch(keyword, warehouseId);
    }

    @Transactional
    public Long create(SaleOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "出货明细不能为空");
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNo = OrderNoUtil.next("SO", orderMapper.maxOrderNo("SO" + today));
        PsiStockOutOrder order = new PsiStockOutOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(dto.getCustomerId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setPaymentType(dto.getPaymentType() == null ? "CASH" : dto.getPaymentType());
        order.setDiscount(nvl(dto.getDiscount()));
        order.setRemark(dto.getRemark());
        order.setSource(dto.getSource() == null ? "PC" : dto.getSource());
        order.setStatus("DRAFT");
        order.setOperatorId(operator.id());
        order.setOperatorName(operator.name());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PsiStockOutItem> items = new ArrayList<>();
        for (SaleOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            BigDecimal qty = nvl(it.getQuantity());
            BigDecimal price = nvl(it.getPrice());
            PsiStockOutItem item = new PsiStockOutItem();
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(qty);
            item.setPrice(price);
            item.setAmount(qty.multiply(price));
            item.setCostPrice(BigDecimal.ZERO);
            item.setRemark(it.getRemark());
            items.add(item);
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        BigDecimal pay = dto.getPayAmount() == null ? totalAmount.subtract(order.getDiscount()) : dto.getPayAmount();
        order.setPayAmount(pay);
        orderMapper.insert(order);
        for (PsiStockOutItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }
        if (Boolean.TRUE.equals(dto.getDirectOut())) {
            outstock(order.getId());
        }
        return order.getId();
    }

    @Transactional
    public void update(Long id, SaleOrderDTO dto) {
        PsiStockOutOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("出货单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可修改");
        }
        itemMapper.delete(new LambdaQueryWrapper<PsiStockOutItem>().eq(PsiStockOutItem::getOrderId, id));
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SaleOrderDTO.Item it : dto.getItems()) {
            BaseBook book = bookMapper.selectById(it.getBookId());
            if (book == null) {
                throw BizException.notFound("图书不存在: " + it.getBookId());
            }
            BigDecimal qty = nvl(it.getQuantity());
            BigDecimal price = nvl(it.getPrice());
            PsiStockOutItem item = new PsiStockOutItem();
            item.setOrderId(id);
            item.setBookId(book.getId());
            item.setBookName(book.getBookName());
            item.setIsbn(book.getIsbn());
            item.setQuantity(qty);
            item.setPrice(price);
            item.setAmount(qty.multiply(price));
            item.setCostPrice(BigDecimal.ZERO);
            itemMapper.insert(item);
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setCustomerId(dto.getCustomerId());
        order.setWarehouseId(dto.getWarehouseId());
        if (dto.getPaymentType() != null) {
            order.setPaymentType(dto.getPaymentType());
        }
        order.setDiscount(nvl(dto.getDiscount()));
        order.setRemark(dto.getRemark());
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(dto.getPayAmount() == null ? totalAmount.subtract(order.getDiscount()) : dto.getPayAmount());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void delete(Long id) {
        PsiStockOutOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("出货单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可删除");
        }
        itemMapper.delete(new LambdaQueryWrapper<PsiStockOutItem>().eq(PsiStockOutItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    @Transactional
    public void submit(Long id) {
        PsiStockOutOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("出货单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw BizException.status("仅草稿状态可提交");
        }
        order.setStatus("SUBMITTED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void cancel(Long id) {
        PsiStockOutOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("出货单不存在");
        }
        orderDocService.assertNotLocked(order.getIsLocked());
        if ("OUTSTOCK".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw BizException.status("当前状态不可作废");
        }
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderDocService.markLocked(order);
        orderMapper.updateById(order);
        orderDocService.recordAction(OrderType.OUT, id, order.getOrderNo(), "CANCEL", "作废出货单");
    }

    /** 确认出库：校验库存、扣减、写流水、累计积分 */
    @Transactional
    public void outstock(Long id) {
        PsiStockOutOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("出货单不存在");
        }
        if (!"DRAFT".equals(order.getStatus()) && !"SUBMITTED".equals(order.getStatus())) {
            throw BizException.status("仅草稿/已提交状态可出库");
        }
        List<PsiStockOutItem> items = itemMapper.selectList(new LambdaQueryWrapper<PsiStockOutItem>()
                .eq(PsiStockOutItem::getOrderId, id));
        Long whId = order.getWarehouseId();
        LocalDateTime now = LocalDateTime.now();

        // 1) 库存校验
        for (PsiStockOutItem item : items) {
            PsiStock stock = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, whId));
            if (stock == null || stock.getQuantity().compareTo(item.getQuantity()) < 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,
                        "库存不足: " + item.getBookName() + " 当前库存 " + (stock == null ? 0 : stock.getQuantity()));
            }
        }
        // 2) 扣减 + 写流水
        for (PsiStockOutItem item : items) {
            PsiStock stock = stockMapper.selectOne(new LambdaQueryWrapper<PsiStock>()
                    .eq(PsiStock::getBookId, item.getBookId())
                    .eq(PsiStock::getWarehouseId, whId));
            BigDecimal before = stock.getQuantity();
            BigDecimal after = before.subtract(item.getQuantity());
            BigDecimal cost = stock.getAvgCost();
            stock.setQuantity(after);
            stock.setLastOutTime(now);
            stock.setUpdateTime(now);
            stockMapper.updateById(stock);

            item.setCostPrice(cost);
            itemMapper.updateById(item);

            PsiStockFlow flow = new PsiStockFlow();
            flow.setBizType("OUT");
            flow.setBizNo(order.getOrderNo());
            flow.setBookId(item.getBookId());
            flow.setWarehouseId(whId);
            flow.setDirection(-1);
            flow.setQuantity(item.getQuantity());
            flow.setBeforeQty(before);
            flow.setAfterQty(after);
            flow.setUnitCost(cost);
            flow.setOperatorId(operator.id());
            flow.setOperatorName(operator.name());
            flow.setSource(order.getSource());
            flow.setCreateTime(now);
            flowMapper.insert(flow);
        }
        // 3) 会员积分
        if (order.getCustomerId() != null) {
            BaseCustomer customer = customerMapper.selectById(order.getCustomerId());
            if (customer != null && customer.getLevelId() != null) {
                BaseMemberLevel level = levelMapper.selectById(customer.getLevelId());
                if (level != null && level.getPointRate() != null) {
                    int add = order.getPayAmount().multiply(level.getPointRate())
                            .setScale(0, RoundingMode.HALF_UP).intValue();
                    BaseCustomer upd = new BaseCustomer();
                    upd.setId(customer.getId());
                    upd.setPoints((customer.getPoints() == null ? 0 : customer.getPoints()) + add);
                    customerMapper.updateById(upd);
                }
            }
        }
        order.setStatus("OUTSTOCK");
        order.setOutTime(now);
        order.setUpdateTime(now);
        orderDocService.markLocked(order);
        orderMapper.updateById(order);
        orderDocService.recordAction(OrderType.OUT, id, order.getOrderNo(), "OUTSTOCK", "确认出库");
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}