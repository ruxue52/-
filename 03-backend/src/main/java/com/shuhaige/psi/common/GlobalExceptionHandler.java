package com.shuhaige.psi.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，返回契约定义的结构与错误码
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：HTTP 200 + 业务错误码 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().hasFieldErrors()) {
            FieldError fe = ex.getBindingResult().getFieldError();
            if (fe != null) {
                msg = fe.getField() + " " + fe.getDefaultMessage();
            }
        } else if (e instanceof BindException ex && ex.getBindingResult().hasFieldErrors()) {
            FieldError fe = ex.getBindingResult().getFieldError();
            if (fe != null) {
                msg = fe.getField() + " " + fe.getDefaultMessage();
            }
        }
        return Result.fail(ErrorCode.PARAM_ERROR, msg);
    }

    /** 其它未捕获异常：HTTP 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SERVER_ERROR, "系统异常"));
    }
}