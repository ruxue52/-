package com.shuhaige.psi.common;

/**
 * 契约错误码表
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    public static final int SUCCESS = 0;
    /** 参数校验失败 */
    public static final int PARAM_ERROR = 1001;
    /** 库存不足 */
    public static final int STOCK_NOT_ENOUGH = 1002;
    /** 单据状态不允许该操作 */
    public static final int STATUS_NOT_ALLOWED = 1003;
    /** 数据不存在 */
    public static final int NOT_FOUND = 1004;
    /** 数据重复 */
    public static final int DUPLICATE = 1005;
    /** 账号或密码错误 */
    public static final int LOGIN_FAIL = 2001;
    /** 账号被停用 */
    public static final int USER_DISABLED = 2002;
    /** 无权限 */
    public static final int NO_PERMISSION = 3001;
    /** 未认证 */
    public static final int UNAUTHORIZED = 401;
    /** 服务异常 */
    public static final int SERVER_ERROR = 500;
}