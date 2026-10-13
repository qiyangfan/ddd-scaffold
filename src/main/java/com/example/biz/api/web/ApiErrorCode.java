package com.example.biz.api.web;

/**
 * api 层错误码：参数校验与系统兜底；领域规则拒绝的码见
 * {@link com.example.biz.domain.exception.DomainErrorCode}。分级沿用 A 用户端 / B 系统执行侧。
 */
public enum ApiErrorCode {

    /** 请求参数非法 */
    PARAM_INVALID("A0400", "invalid request parameter"),
    /** 系统内部错误 */
    SYSTEM_ERROR("B0001", "internal system error");

    private final String code;
    private final String message;

    ApiErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
