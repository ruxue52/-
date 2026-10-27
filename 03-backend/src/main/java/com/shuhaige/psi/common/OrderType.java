package com.shuhaige.psi.common;

import java.util.Set;

/**
 * 单据类型。取值与 psi_order_revision.order_type / psi_order_action_log.order_type 对齐。
 * 状态机按类型分别枚举：盘点/退货无 SUBMITTED，调拨独有 PENDING，故不写成通用集合。
 */
public enum OrderType {

    /** 进货单：DRAFT/SUBMITTED -> INSTOCK；终态 INSTOCK、CANCELLED */
    IN("INSTOCK", "CANCELLED"),
    /** 出货单：DRAFT/SUBMITTED -> OUTSTOCK；终态 OUTSTOCK、CANCELLED */
    OUT("OUTSTOCK", "CANCELLED"),
    /** 盘点单：DRAFT -> FINISHED；终态 FINISHED、CANCELLED */
    CHECK("FINISHED", "CANCELLED"),
    /** 调拨单：DRAFT/PENDING -> TRANSFERRED；终态 TRANSFERRED、CANCELLED */
    TRANSFER("TRANSFERRED", "CANCELLED"),
    /** 退货单：DRAFT -> FINISHED；终态 FINISHED、CANCELLED */
    RETURN("FINISHED", "CANCELLED");

    private final Set<String> terminalStatuses;

    OrderType(String... terminalStatuses) {
        this.terminalStatuses = Set.of(terminalStatuses);
    }

    public static OrderType of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "单据类型不能为空");
        }
        try {
            return OrderType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知单据类型: " + value);
        }
    }

    /** 终态（已执行或已作废）：不可确认、不可编辑、不可申请修订 */
    public boolean isTerminal(String status) {
        return terminalStatuses.contains(status);
    }

    /** 终态集合，供归档批量更新按类型取状态列表 */
    public Set<String> terminalStatuses() {
        return terminalStatuses;
    }

    /** 中文展示名 */
    public String label() {
        return switch (this) {
            case IN -> "进货单";
            case OUT -> "出货单";
            case CHECK -> "盘点单";
            case TRANSFER -> "调拨单";
            case RETURN -> "退货单";
        };
    }

    /** 可编辑：5 类单据均仅草稿状态可改 */
    public boolean isEditable(String status) {
        return "DRAFT".equals(status);
    }

    /** 可确认：尚未进入终态 */
    public boolean isConfirmable(String status) {
        return !isTerminal(status);
    }
}
