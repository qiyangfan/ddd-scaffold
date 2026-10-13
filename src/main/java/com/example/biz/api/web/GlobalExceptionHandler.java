package com.example.biz.api.web;

import com.example.biz.domain.exception.DomainException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 全局异常处理：领域异常在此翻译为统一响应体——domain 层保持框架无关的关键装配点。
 * <p>约定：领域/业务异常 HTTP 200 + 业务码；参数错误 400；未知错误 500（堆栈只进日志）。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse(ApiErrorCode.PARAM_INVALID.getMessage());
        log.warn("parameter validation failed: {}", message);
        return R.fail(ApiErrorCode.PARAM_INVALID.getCode(), message);
    }

    /** @PathVariable / @RequestParam 上的约束校验失败（Spring 6.1+ 方法级校验） */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        log.warn("handler method validation failed: {}", e.getMessage());
        return R.fail(ApiErrorCode.PARAM_INVALID.getCode(), ApiErrorCode.PARAM_INVALID.getMessage());
    }

    /** 方法参数级 @Validated 约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("constraint violation: {}", e.getMessage());
        return R.fail(ApiErrorCode.PARAM_INVALID.getCode(), ApiErrorCode.PARAM_INVALID.getMessage());
    }

    /** 请求体不可读（JSON 格式错误等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("request body not readable: {}", e.getMessage());
        return R.fail(ApiErrorCode.PARAM_INVALID.getCode(), ApiErrorCode.PARAM_INVALID.getMessage());
    }

    /** 领域异常：业务规则拒绝，正常业务流内不打 error 级别日志 */
    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleDomainException(DomainException e) {
        log.info("domain exception, code:{}, message:{}", e.getErrorCode().getCode(), e.getMessage());
        return R.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    /** 未知异常：兜底，堆栈只进日志，响应只透出通用文案 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleUnknown(Exception e) {
        log.error("unexpected error", e);
        return R.fail(ApiErrorCode.SYSTEM_ERROR.getCode(), ApiErrorCode.SYSTEM_ERROR.getMessage());
    }
}
