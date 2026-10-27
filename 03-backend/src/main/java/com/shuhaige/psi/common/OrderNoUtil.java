package com.shuhaige.psi.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 单号工具：前缀 + yyyyMMdd + 3 位当日流水
 */
public final class OrderNoUtil {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderNoUtil() {
    }

    /**
     * @param prefix     单号前缀，如 PI / SO / CK / TR / RT
     * @param maxTodayNo 当日已存在的最大单号（可为 null）
     */
    public static String next(String prefix, String maxTodayNo) {
        String day = LocalDate.now().format(DAY);
        String base = prefix + day;
        int seq = 1;
        if (maxTodayNo != null && maxTodayNo.startsWith(base)) {
            String tail = maxTodayNo.substring(base.length());
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return base + String.format("%03d", seq);
    }
}