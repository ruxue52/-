package com.shuhaige.psi.common;

import lombok.Getter;

/**
 * 业务异常，携带契约错误码
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ErrorCode.PARAM_ERROR;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(int code, String message) {
        return new BizException(code, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, message);
    }

    public static BizException status(String message) {
        return new BizException(ErrorCode.STATUS_NOT_ALLOWED, message);
    }
}