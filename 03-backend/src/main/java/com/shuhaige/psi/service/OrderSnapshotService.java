package com.shuhaige.psi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.OrderType;
import com.shuhaige.psi.entity.*;
import com.shuhaige.psi.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单据快照采集：把单据头 + 明细序列化为 JSON 字符串，供修订前后对比。
 * <p>只取业务字段（不直接序列化实体），避免今后实体加字段/加审计列污染历史对比。
 * <p>用 LONGTEXT 存字符串而非 MySQL 原生 JSON 类型，以保持到达梦(DM8)的迁移兼容性。
 */
@Service
@RequiredArgsConstructor
public class OrderSnapshotService {

    private final ObjectMapper objectMapper;
    private final PsiStockInOrderMapper inOrderMapper;
    private final PsiStockInItemMapper inItemMapper;
    private final PsiStockOutOrderMapper outOrderMapper;
    private final PsiStockOutItemMapper outItemMapper;
    private final PsiStockCheckOrderMapper checkOrderMapper;
    private final PsiStockCheckItemMapper checkItemMapper;
    private final PsiStockTransferOrderMapper transferOrderMapper;
    private final PsiStockTransferItemMapper transferItemMapper;
    private final PsiReturnOrderMapper returnOrderMapper;
    private final PsiReturnItemMapper returnItemMapper;

    public String capture(OrderType type, Long id) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        switch (type) {
            case IN: {
                PsiStockInOrder o = inOrderMapper.selectById(id);
                if (o == null) {
                    throw BizException.notFound("进货单不存在");
                }
                data.put("header", header(o.getOrderNo(), o.getStatus(), o.getWarehouseId(),
                        o.getSupplierId(), null, o.getTotalQty(), o.getTotalAmount(),
                        o.getPayAmount(), o.getDiscount(), o.getSettleType(), o.getRemark()));
                for (PsiStockInItem i : inItemMapper.selectList(new LambdaQueryWrapper<PsiStockInItem>()
                        .eq(PsiStockInItem::getOrderId, id).orderByAsc(PsiStockInItem::getId))) {
                    Map<String, Object> m = itemBase(i.getBookId(), i.getBookName(), i.getIsbn(), i.getQuantity(), i.getRemark());
                    m.put("price", i.getPrice());
                    m.put("amount", i.getAmount());
                    m.put("batchNo", i.getBatchNo());
                    items.add(m);
                }
                break;
            }
            case OUT: {
                PsiStockOutOrder o = outOrderMapper.selectById(id);
                if (o == null) {
                    throw BizException.notFound("出货单不存在");
                }
                data.put("header", header(o.getOrderNo(), o.getStatus(), o.getWarehouseId(),
                        null, o.getCustomerId(), o.getTotalQty(), o.getTotalAmount(),
                        o.getPayAmount(), o.getDiscount(), o.getPaymentType(), o.getRemark()));
                for (PsiStockOutItem i : outItemMapper.selectList(new LambdaQueryWrapper<PsiStockOutItem>()
                        .eq(PsiStockOutItem::getOrderId, id).orderByAsc(PsiStockOutItem::getId))) {
                    Map<String, Object> m = itemBase(i.getBookId(), i.getBookName(), i.getIsbn(), i.getQuantity(), i.getRemark());
                    m.put("price", i.getPrice());
                    m.put("amount", i.getAmount());
                    items.add(m);
                }
                break;
            }
            case CHECK: {
                PsiStockCheckOrder o = checkOrderMapper.selectById(id);
                if (o == null) {
                    throw BizException.notFound("盘点单不存在");
                }
                data.put("header", header(o.getCheckNo(), o.getStatus(), o.getWarehouseId(),
                        null, null, null, null, null, null, o.getCheckType(), o.getRemark()));
                for (PsiStockCheckItem i : checkItemMapper.selectList(new LambdaQueryWrapper<PsiStockCheckItem>()
                        .eq(PsiStockCheckItem::getOrderId, id).orderByAsc(PsiStockCheckItem::getId))) {
                    Map<String, Object> m = itemBase(i.getBookId(), i.getBookName(), i.getIsbn(), i.getRealQty(), i.getRemark());
                    m.put("bookQty", i.getBookQty());
                    m.put("diffQty", i.getDiffQty());
                    m.put("diffAmount", i.getDiffAmount());
                    items.add(m);
                }
                break;
            }
            case TRANSFER: {
                PsiStockTransferOrder o = transferOrderMapper.selectById(id);
                if (o == null) {
                    throw BizException.notFound("调拨单不存在");
                }
                Map<String, Object> h = header(o.getTransferNo(), o.getStatus(), o.getFromWarehouse(),
                        null, null, o.getTotalQty(), null, null, null, null, o.getRemark());
                h.put("toWarehouseId", o.getToWarehouse());
                data.put("header", h);
                for (PsiStockTransferItem i : transferItemMapper.selectList(new LambdaQueryWrapper<PsiStockTransferItem>()
                        .eq(PsiStockTransferItem::getOrderId, id).orderByAsc(PsiStockTransferItem::getId))) {
                    items.add(itemBase(i.getBookId(), i.getBookName(), i.getIsbn(), i.getQuantity(), null));
                }
                break;
            }
            case RETURN: {
                PsiReturnOrder o = returnOrderMapper.selectById(id);
                if (o == null) {
                    throw BizException.notFound("退货单不存在");
                }
                Map<String, Object> h = header(o.getReturnNo(), o.getStatus(), o.getWarehouseId(),
                        null, o.getPartnerId(), o.getTotalQty(), o.getTotalAmount(), null, null,
                        o.getReturnType(), o.getReason());
                h.put("partnerName", o.getPartnerName());
                data.put("header", h);
                for (PsiReturnItem i : returnItemMapper.selectList(new LambdaQueryWrapper<PsiReturnItem>()
                        .eq(PsiReturnItem::getOrderId, id).orderByAsc(PsiReturnItem::getId))) {
                    Map<String, Object> m = itemBase(i.getBookId(), i.getBookName(), i.getIsbn(), i.getQuantity(), null);
                    m.put("price", i.getPrice());
                    m.put("amount", i.getAmount());
                    items.add(m);
                }
                break;
            }
            default:
                throw new BizException("不支持的单据类型");
        }
        data.put("items", items);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new BizException("单据快照生成失败");
        }
    }

    private Map<String, Object> header(String no, String status, Long warehouseId, Long supplierId, Long customerId,
                                       BigDecimal totalQty, BigDecimal totalAmount, BigDecimal payAmount,
                                       BigDecimal discount, String bizType, String remark) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("bizNo", no);
        h.put("status", status);
        h.put("warehouseId", warehouseId);
        h.put("supplierId", supplierId);
        h.put("customerId", customerId);
        h.put("totalQty", totalQty);
        h.put("totalAmount", totalAmount);
        h.put("payAmount", payAmount);
        h.put("discount", discount);
        h.put("bizType", bizType);
        h.put("remark", remark);
        return h;
    }

    private Map<String, Object> itemBase(Long bookId, String bookName, String isbn, BigDecimal qty, String remark) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookId", bookId);
        m.put("bookName", bookName);
        m.put("isbn", isbn);
        m.put("quantity", qty);
        m.put("remark", remark);
        return m;
    }
}
