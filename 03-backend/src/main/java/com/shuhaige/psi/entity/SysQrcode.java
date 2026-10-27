package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_qrcode")
public class SysQrcode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private String bizId;
    private String qrContent;
    private String qrUrl;
    private Integer scanCount;
    private LocalDateTime createTime;
}