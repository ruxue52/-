package com.shuhaige.psi.controller;

import cn.hutool.extra.qrcode.QrCodeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.SysQrcode;
import com.shuhaige.psi.mapper.SysQrcodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * 二维码：图书 / 单据 / 内容
 */
@RestController
@RequestMapping("/qrcode")
@RequiredArgsConstructor
public class QrcodeController {

    private final SysQrcodeMapper qrcodeMapper;

    @GetMapping(value = "/book/{isbn}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> bookQr(@PathVariable String isbn) {
        String content = "SHG:BOOK:" + isbn;
        saveQr("BOOK", isbn, content);
        return png(content);
    }

    @GetMapping(value = "/order/{orderNo}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> orderQr(@PathVariable String orderNo) {
        String content = "SHG:ORDER:" + orderNo;
        saveQr("ORDER", orderNo, content);
        return png(content);
    }

    @GetMapping("/content/{code}")
    public Result<String> content(@PathVariable String code) {
        return Result.ok(resolveContent(code));
    }

    private String resolveContent(String code) {
        if (code.startsWith("SHG:")) {
            return code;
        }
        if (code.startsWith("PI") || code.startsWith("SO") || code.startsWith("CK")
                || code.startsWith("TR") || code.startsWith("RT")) {
            return "SHG:ORDER:" + code;
        }
        return "SHG:BOOK:" + code;
    }

    private void saveQr(String bizType, String bizId, String content) {
        Long exists = qrcodeMapper.selectCount(new LambdaQueryWrapper<SysQrcode>()
                .eq(SysQrcode::getBizType, bizType).eq(SysQrcode::getBizId, bizId));
        if (exists == null || exists == 0) {
            SysQrcode qr = new SysQrcode();
            qr.setBizType(bizType);
            qr.setBizId(bizId);
            qr.setQrContent(content);
            qr.setScanCount(0);
            qrcodeMapper.insert(qr);
        }
    }

    private ResponseEntity<byte[]> png(String content) {
        BufferedImage image = QrCodeUtil.generate(content, 300, 300);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("二维码生成失败: " + e.getMessage(), e);
        }
    }
}