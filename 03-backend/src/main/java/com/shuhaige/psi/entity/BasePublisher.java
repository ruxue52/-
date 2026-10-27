package com.shuhaige.psi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("base_publisher")
public class BasePublisher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String publisherName;
    private String contact;
    private String phone;
    private String address;
    private Integer status;
    private LocalDateTime createTime;
}