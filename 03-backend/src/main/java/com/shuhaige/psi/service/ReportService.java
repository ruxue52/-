package com.shuhaige.psi.service;

import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统计报表 R01~R15 组装与导出
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    public Map<String, Object> build(String code, Map<String, String> p) {
        String title = null;
        List<Map<String, String>> columns = null;
        List<Map<String, Object>> rows = null;
        List<String> sumKeys = new ArrayList<>();

        switch (code.toUpperCase()) {
            case "R01" -> {
                title = "销售日报表";
                columns = cols("orderNo", "单号", "outTime", "出库时间", "customerName", "客户",
                        "totalQty", "数量", "totalAmount", "金额", "payAmount", "实收",
                        "paymentType", "支付方式", "operatorName", "收银员");
                rows = reportMapper.r01(defaultStr(p.get("date"),
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
                sumKeys = List.of("totalQty", "totalAmount", "payAmount");
            }
            case "R02" -> {
                title = "销售月报表";
                columns = cols("date", "日期", "orderCount", "单数", "totalQty", "数量",
                        "totalAmount", "金额", "payAmount", "实收");
                rows = reportMapper.r02(defaultStr(p.get("month"),
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
                sumKeys = List.of("orderCount", "totalQty", "totalAmount", "payAmount");
            }
            case "R03" -> {
                title = "商品销售排行榜";
                columns = cols("isbn", "ISBN", "bookName", "书名", "author", "作者", "qty", "销量", "amount", "销售额");
                rows = reportMapper.r03(p.get("startDate"), p.get("endDate"), intOf(p.get("limit"), 20));
                sumKeys = List.of("qty", "amount");
            }
            case "R04" -> {
                title = "分类销售汇总表";
                columns = cols("categoryName", "分类", "skuCount", "品种数", "qty", "数量", "amount", "金额");
                rows = reportMapper.r04(p.get("startDate"), p.get("endDate"));
                sumKeys = List.of("qty", "amount");
            }
            case "R05" -> {
                title = "采购到货明细表";
                columns = cols("orderNo", "单号", "inTime", "到货时间", "supplierName", "供应商", "isbn", "ISBN",
                        "bookName", "书名", "quantity", "数量", "price", "单价", "amount", "金额");
                rows = reportMapper.r05(p.get("startDate"), p.get("endDate"), longOf(p.get("supplierId")));
                sumKeys = List.of("quantity", "amount");
            }
            case "R06" -> {
                title = "供应商采购汇总表";
                columns = cols("supplierName", "供应商", "contact", "联系人", "phone", "电话", "orderCount", "单数",
                        "totalQty", "数量", "totalAmount", "金额", "payAmount", "实付");
                rows = reportMapper.r06(p.get("startDate"), p.get("endDate"));
                sumKeys = List.of("totalQty", "totalAmount", "payAmount");
            }
            case "R07" -> {
                title = "库存结存表";
                columns = cols("isbn", "ISBN", "bookName", "书名", "warehouseName", "仓库", "categoryName", "分类",
                        "quantity", "结存数量", "safetyStock", "安全库存", "avgCost", "平均成本", "stockAmount", "金额");
                rows = reportMapper.r07(longOf(p.get("warehouseId")), longOf(p.get("categoryId")));
                sumKeys = List.of("quantity", "stockAmount");
            }
            case "R08" -> {
                title = "库存预警表";
                columns = cols("isbn", "ISBN", "bookName", "书名", "warehouseName", "仓库", "quantity", "当前库存",
                        "safetyStock", "安全库存", "shortageQty", "缺口");
                rows = reportMapper.r08(longOf(p.get("warehouseId")));
                sumKeys = List.of("shortageQty");
            }
            case "R09" -> {
                title = "库龄分析表";
                columns = cols("isbn", "ISBN", "bookName", "书名", "warehouseName", "仓库", "quantity", "库存",
                        "lastInTime", "最后入库", "ageDays", "库龄(天)", "stockAmount", "金额");
                rows = reportMapper.r09(longOf(p.get("warehouseId")));
                sumKeys = List.of("quantity", "stockAmount");
            }
            case "R10" -> {
                title = "盘点差异表";
                columns = cols("checkNo", "盘点单号", "warehouseName", "仓库", "isbn", "ISBN", "bookName", "书名",
                        "bookQty", "账面数", "realQty", "实盘数", "diffQty", "差异", "diffAmount", "差异金额");
                rows = reportMapper.r10(p.get("checkNo"), p.get("startDate"), p.get("endDate"));
                sumKeys = List.of("diffQty", "diffAmount");
            }
            case "R11" -> {
                title = "商品毛利分析表";
                columns = cols("isbn", "ISBN", "bookName", "书名", "qty", "销量", "amount", "销售额",
                        "costAmount", "成本", "profit", "毛利", "profitRate", "毛利率(%)");
                rows = reportMapper.r11(p.get("startDate"), p.get("endDate"));
                sumKeys = List.of("qty", "amount", "costAmount", "profit");
            }
            case "R12" -> {
                title = "客户消费明细表";
                columns = cols("orderNo", "单号", "outTime", "时间", "customerName", "客户", "totalQty", "数量",
                        "totalAmount", "金额", "discount", "优惠", "payAmount", "实收", "paymentType", "支付方式");
                rows = reportMapper.r12(longOf(p.get("customerId")), p.get("startDate"), p.get("endDate"));
                sumKeys = List.of("totalQty", "totalAmount", "payAmount");
            }
            case "R13" -> {
                title = "会员积分报表";
                columns = cols("customerCode", "会员编码", "customerName", "会员", "levelName", "等级",
                        "points", "积分", "balance", "余额", "phone", "电话");
                rows = reportMapper.r13(longOf(p.get("levelId")));
                sumKeys = List.of("points");
            }
            case "R14" -> {
                title = "库存流水台账";
                columns = cols("bizType", "业务类型", "bizNo", "业务单号", "isbn", "ISBN", "bookName", "书名",
                        "warehouseName", "仓库", "direction", "方向", "quantity", "数量", "beforeQty", "变动前",
                        "afterQty", "变动后", "unitCost", "单位成本", "createTime", "发生时间");
                rows = reportMapper.r14(longOf(p.get("bookId")), longOf(p.get("warehouseId")),
                        p.get("startDate"), p.get("endDate"));
            }
            case "R15" -> {
                title = "进销存汇总表";
                columns = cols("isbn", "ISBN", "bookName", "书名", "warehouseName", "仓库", "inQty", "入库",
                        "outQty", "出库", "endQty", "结存", "stockAmount", "结存金额");
                rows = reportMapper.r15(defaultStr(p.get("month"),
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))), longOf(p.get("warehouseId")));
                sumKeys = List.of("inQty", "outQty", "endQty", "stockAmount");
            }
            default -> throw new BizException(ErrorCode.NOT_FOUND, "报表编号不存在: " + code);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", title);
        data.put("columns", columns);
        data.put("rows", rows);
        data.put("summary", summarize(rows, sumKeys));
        return data;
    }

    /** 导出 xlsx 或 csv 字节流 */
    public byte[] export(Map<String, Object> report, String format) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> columns = (List<Map<String, String>>) report.get("columns");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) report.get("rows");
        String title = String.valueOf(report.get("title"));
        if ("csv".equalsIgnoreCase(format)) {
            return toCsv(columns, rows).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return toXlsx(title, columns, rows);
    }

    private byte[] toXlsx(String title, List<Map<String, String>> columns, List<Map<String, Object>> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(title);
            Row head = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                head.createCell(i).setCellValue(columns.get(i).get("label"));
            }
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row dataRow = sheet.createRow(r++);
                for (int i = 0; i < columns.size(); i++) {
                    Object v = row.get(columns.get(i).get("key"));
                    Cell cell = dataRow.createCell(i);
                    if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(v == null ? "" : v.toString());
                    }
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private String toCsv(List<Map<String, String>> columns, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(columns.get(i).get("label"));
        }
        sb.append('\n');
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                Object v = row.get(columns.get(i).get("key"));
                String s = v == null ? "" : v.toString();
                if (s.contains(",") || s.contains("\"")) {
                    s = "\"" + s.replace("\"", "\"\"") + "\"";
                }
                sb.append(s);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private Map<String, Object> summarize(List<Map<String, Object>> rows, List<String> sumKeys) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("rowCount", rows.size());
        for (String key : sumKeys) {
            BigDecimal total = BigDecimal.ZERO;
            for (Map<String, Object> row : rows) {
                Object v = row.get(key);
                if (v instanceof Number n) {
                    total = total.add(new BigDecimal(n.toString()));
                }
            }
            summary.put(key, total);
        }
        return summary;
    }

    private List<Map<String, String>> cols(String... kv) {
        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", kv[i]);
            m.put("label", kv[i + 1]);
            list.add(m);
        }
        return list;
    }

    private String defaultStr(String v, String def) {
        return v == null || v.isEmpty() ? def : v;
    }

    private Long longOf(String v) {
        if (v == null || v.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intOf(String v, int def) {
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}