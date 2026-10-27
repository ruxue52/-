package com.shuhaige.psi.dto;

import lombok.Data;

@Data
public class ScanDTO {
    private String code;
    private String bizType;
    private String bizId;
    private String device;
    private String result;
}