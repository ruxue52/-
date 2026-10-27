package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.security.RequirePerm;
import com.shuhaige.psi.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{code}")
    @RequirePerm(prefix = "report:", pathVar = "code")
    public Result<Map<String, Object>> report(@PathVariable String code,
                                              @RequestParam Map<String, String> params) {
        return Result.ok(reportService.build(code, params));
    }

    @GetMapping("/{code}/export")
    @RequirePerm(prefix = "report:", pathVar = "code")
    public ResponseEntity<byte[]> export(@PathVariable String code,
                                         @RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam Map<String, String> params) {
        Map<String, Object> report = reportService.build(code, params);
        byte[] bytes = reportService.export(report, format);
        String title = String.valueOf(report.get("title"));
        String ext = "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
        String filename = URLEncoder.encode(title + "." + ext, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType("csv".equalsIgnoreCase(format)
                ? new MediaType("text", "csv", StandardCharsets.UTF_8)
                : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}