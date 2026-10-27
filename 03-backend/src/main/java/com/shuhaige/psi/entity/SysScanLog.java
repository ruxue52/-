package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_scan_log")
public class SysScanLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String bizType;
    private String bizId;
    private String device;
    private Long userId;
    private String result;
    private LocalDateTime createTime;
}