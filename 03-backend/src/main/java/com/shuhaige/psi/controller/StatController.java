package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stat")
@RequiredArgsConstructor
public class StatController {

    private final ReportMapper reportMapper;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(reportMapper.overview());
    }

    @GetMapping("/sales/trend")
    public Result<List<Map<String, Object>>> salesTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(reportMapper.salesTrend(days));
    }

    @GetMapping("/sales/monthly")
    public Result<List<Map<String, Object>>> salesMonthly(@RequestParam(defaultValue = "2026") int year) {
        return Result.ok(reportMapper.salesMonthly(year));
    }

    @GetMapping("/sales/top-books")
    public Result<List<Map<String, Object>>> salesTopBooks(@RequestParam(defaultValue = "10") Integer limit,
                                                           @RequestParam(required = false) String startDate,
                                                           @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.salesTopBooks(limit, startDate, endDate));
    }

    @GetMapping("/sales/by-category")
    public Result<List<Map<String, Object>>> salesByCategory(@RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.salesByCategory(startDate, endDate));
    }

    @GetMapping("/sales/by-publisher")
    public Result<List<Map<String, Object>>> salesByPublisher(@RequestParam(required = false) String startDate,
                                                              @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.salesByPublisher(startDate, endDate));
    }

    @GetMapping("/sales/by-warehouse")
    public Result<List<Map<String, Object>>> salesByWarehouse(@RequestParam(required = false) String startDate,
                                                              @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.salesByWarehouse(startDate, endDate));
    }

    @GetMapping("/sales/by-payment")
    public Result<List<Map<String, Object>>> salesByPayment(@RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.salesByPayment(startDate, endDate));
    }

    @GetMapping("/sales/by-operator")
    public Result<List<Map<String, Object>>> salesByOperator(@RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.salesByOperator(startDate, endDate));
    }

    @GetMapping("/sales/hour")
    public Result<List<Map<String, Object>>> salesByHour(@RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.salesByHour(startDate, endDate));
    }

    @GetMapping("/purchase/trend")
    public Result<List<Map<String, Object>>> purchaseTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(reportMapper.purchaseTrend(days));
    }

    @GetMapping("/purchase/by-supplier")
    public Result<List<Map<String, Object>>> purchaseBySupplier(@RequestParam(required = false) String startDate,
                                                                @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.purchaseBySupplier(startDate, endDate));
    }

    @GetMapping("/purchase/top-books")
    public Result<List<Map<String, Object>>> purchaseTopBooks(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(reportMapper.purchaseTopBooks(limit));
    }

    @GetMapping("/stock/value-by-category")
    public Result<List<Map<String, Object>>> stockValueByCategory() {
        return Result.ok(reportMapper.stockValueByCategory());
    }

    @GetMapping("/stock/turnover")
    public Result<Map<String, Object>> stockTurnover(@RequestParam(required = false) String startDate,
                                                     @RequestParam(required = false) String endDate) {
        Map<String, Object> data = reportMapper.stockTurnover(startDate, endDate);
        BigDecimal saleCost = toDecimal(data.get("saleCost"));
        BigDecimal avgStock = toDecimal(data.get("avgStockAmount"));
        if (avgStock.compareTo(BigDecimal.ZERO) > 0) {
            data.put("turnoverRate", saleCost.divide(avgStock, 4, RoundingMode.HALF_UP));
        } else {
            data.put("turnoverRate", BigDecimal.ZERO);
        }
        return Result.ok(data);
    }

    @GetMapping("/stock/age")
    public Result<List<Map<String, Object>>> stockAge() {
        return Result.ok(reportMapper.stockAge());
    }

    @GetMapping("/profit/by-book")
    public Result<List<Map<String, Object>>> profitByBook(@RequestParam(defaultValue = "10") Integer limit,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.profitByBook(limit, startDate, endDate));
    }

    @GetMapping("/profit/monthly")
    public Result<List<Map<String, Object>>> profitMonthly(@RequestParam(defaultValue = "2026") int year) {
        return Result.ok(reportMapper.profitMonthly(year));
    }

    @GetMapping("/customer/consume-rank")
    public Result<List<Map<String, Object>>> consumeRank(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(reportMapper.customerConsumeRank(limit));
    }

    @GetMapping("/customer/active")
    public Result<Map<String, Object>> customerActive(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(reportMapper.customerActive(days));
    }

    @GetMapping("/member/level-distribution")
    public Result<List<Map<String, Object>>> levelDistribution() {
        return Result.ok(reportMapper.memberLevelDistribution());
    }

    @GetMapping("/check/diff-summary")
    public Result<List<Map<String, Object>>> checkDiffSummary(@RequestParam(required = false) String startDate,
                                                              @RequestParam(required = false) String endDate) {
        return Result.ok(reportMapper.checkDiffSummary(startDate, endDate));
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(v.toString());
    }
}