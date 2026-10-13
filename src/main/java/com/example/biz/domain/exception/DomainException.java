package com.example.biz.domain.exception;

/**
 * 领域异常：业务规则在领域层被拒绝时的唯一出口，由 api 层全局异常处理器转换为统一响应体。
 * <p>纯 JDK 实现，domain 层不感知 Spring/Web。</p>
 */
public class DomainException extends RuntimeException {

    private final DomainErrorCode errorCode;

    public DomainException(DomainErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DomainException(DomainErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainErrorCode getErrorCode() {
        return errorCode;
    }
}
