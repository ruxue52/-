package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.BizException;
import com.shuhaige.psi.common.ErrorCode;
import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.entity.BaseCustomer;
import com.shuhaige.psi.security.JwtUtil;
import com.shuhaige.psi.security.LoginUser;
import com.shuhaige.psi.service.ReaderCodeService;
import com.shuhaige.psi.service.ReaderMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 读者端（会员）认证：手机号 + 短信验证码登录
 * 与员工端 /auth/login 完全分离，签发 userType=CUSTOMER 的 token
 */
@RestController
@RequestMapping("/reader/auth")
@RequiredArgsConstructor
public class ReaderAuthController {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final ReaderMemberService memberService;
    private final ReaderCodeService codeService;
    private final JwtUtil jwtUtil;

    /** 演示环境是否在响应中回显验证码（生产接入短信网关后应置为 false） */
    @Value("${reader.auth.echo-code:true}")
    private boolean echoCode;

    @PostMapping("/send-code")
    public Result<Map<String, Object>> sendCode(@RequestBody Map<String, String> body) {
        String phone = normalizePhone(body.get("phone"));
        BaseCustomer customer = memberService.findByPhone(phone);
        if (customer == null) {
            throw BizException.notFound("该手机号尚未注册会员，请先到门店办理会员卡");
        }
        String code = codeService.send(phone);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phone", phone);
        data.put("expireMinutes", ReaderCodeService.EXPIRE_MINUTES);
        if (echoCode) {
            // 未接短信网关，回显验证码以便演示/联调
            data.put("code", code);
        }
        return Result.ok(data);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String phone = normalizePhone(body.get("phone"));
        if (!codeService.verify(phone, body.get("code"))) {
            throw new BizException(ErrorCode.LOGIN_FAIL, "验证码错误或已过期");
        }
        BaseCustomer customer = memberService.findByPhone(phone);
        if (customer == null) {
            throw new BizException(ErrorCode.LOGIN_FAIL, "会员不存在或已停用");
        }
        String token = jwtUtil.generate(customer.getId(), customer.getCustomerCode(), LoginUser.TYPE_CUSTOMER);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("member", memberService.memberView(customer));
        return Result.ok(data);
    }

    private String normalizePhone(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (!PHONE.matcher(value).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请输入正确的 11 位手机号");
        }
        return value;
    }
}
