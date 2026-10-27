package com.shuhaige.psi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 晓镜书阁 · 书店进销存管理系统 启动类
 */
@SpringBootApplication
@MapperScan("com.shuhaige.psi.mapper")
public class PsiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsiApplication.class, args);
    }
}