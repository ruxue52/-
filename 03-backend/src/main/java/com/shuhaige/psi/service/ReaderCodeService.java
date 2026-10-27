package com.shuhaige.psi.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 读者端（会员）手机验证码。
 * 演示环境未接短信网关，验证码由接口直接回显，短信下发在接入网关后替换 send 的实现即可。
 */
@Service
public class ReaderCodeService {

    /** 验证码有效期（分钟） */
    public static final int EXPIRE_MINUTES = 5;

    private record CodeEntry(String code, long expireAt) {
    }

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

    /** 生成并保存验证码，返回明文（由调用方决定是否回显） */
    public String send(String phone) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        store.put(phone, new CodeEntry(code, System.currentTimeMillis() + EXPIRE_MINUTES * 60_000L));
        return code;
    }

    /** 校验验证码，校验通过后立即失效 */
    public boolean verify(String phone, String code) {
        CodeEntry entry = store.get(phone);
        if (entry == null || code == null || code.isBlank()) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            store.remove(phone);
            return false;
        }
        if (!entry.code().equals(code.trim())) {
            return false;
        }
        store.remove(phone);
        return true;
    }
}
