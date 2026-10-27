package com.shuhaige.psi.entity;

import java.time.LocalDateTime;

/**
 * 具备「确认锁定」语义的单据统一接口。
 * 5 类单据主表（进货/出货/盘点/调拨/退货）均实现本接口，
 * 使确认锁定与修订审批的守卫逻辑只需写一份。
 */
public interface LockAware {

    Long getId();

    /** 业务单号：各表列名不同（order_no / check_no / transfer_no / return_no） */
    String bizNo();

    String getStatus();

    /** 是否已确认锁定 1是 0否 */
    Integer getIsLocked();

    void setIsLocked(Integer isLocked);

    Long getConfirmUserId();

    void setConfirmUserId(Long confirmUserId);

    String getConfirmUserName();

    void setConfirmUserName(String confirmUserName);

    LocalDateTime getConfirmTime();

    void setConfirmTime(LocalDateTime confirmTime);

    /** 是否已归档 1是 0否 */
    Integer getIsArchived();

    void setIsArchived(Integer isArchived);

    LocalDateTime getArchiveTime();

    void setArchiveTime(LocalDateTime archiveTime);

    Long getArchiveUserId();

    void setArchiveUserId(Long archiveUserId);

    String getArchiveUserName();

    void setArchiveUserName(String archiveUserName);
}
