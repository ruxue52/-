package com.shuhaige.psi.controller;

import com.shuhaige.psi.common.Result;
import com.shuhaige.psi.dto.LoginDTO;
import com.shuhaige.psi.security.UserContext;
import com.shuhaige.psi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(authService.me(UserContext.getUserId()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        UserContext.clear();
        return Result.ok();
    }
}