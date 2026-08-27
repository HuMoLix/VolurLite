package com.humolix.volur.framework.common.exception.handler;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败 (JSON): {}", message);
        return ApiResponseEntity.badRequest(message);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHandlerMethodValidationException(org.springframework.web.method.annotation.HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .collect(Collectors.joining(", "));
        log.warn("方法参数校验失败: {}", message);
        return ApiResponseEntity.badRequest(message.isBlank() ? "请求参数校验未通过" : message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return ApiResponseEntity.badRequest(message.isBlank() ? "请求参数格式不正确" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数约束校验失败: {}", message);
        return ApiResponseEntity.badRequest(message.isBlank() ? "请求参数不符合约束要求" : message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleBadCredentialsException() {
        return ApiResponseEntity.unauthorized("用户名或密码错误");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException() {
        return ApiResponseEntity.forbidden("权限不足，拒绝访问");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFoundException() {
        return ApiResponseEntity.notFound("访问的资源不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return ApiResponseEntity.methodNotAllowed("不支持该请求方式: " + ex.getMethod());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("请求体读取失败: {}", ex.getMessage());
        return ApiResponseEntity.badRequest("请求体格式不正确或缺少请求体");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return ApiResponseEntity.badRequest("缺少必要的请求参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return ApiResponseEntity.badRequest("请求参数类型不匹配: " + ex.getName());
    }

    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMultipartException(Exception ex) {
        log.warn("文件上传异常: {}", ex.getMessage());
        return ApiResponseEntity.badRequest("文件上传失败，请检查上传文件大小与格式是否符合要求");
    }

    @ExceptionHandler({ClientAbortException.class, org.springframework.web.context.request.async.AsyncRequestNotUsableException.class})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleClientAbortException(Exception ex) {
        log.debug("客户端主动断开长连接或异步请求已关闭: {}", ex.getMessage());
    }

    @ExceptionHandler(MailException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleMailException(MailException ex) {
        log.error("邮件服务异常: ", ex);
        return ApiResponseEntity.error(HttpStatus.INTERNAL_SERVER_ERROR, "邮件服务暂时不可用，请稍后再试");
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: status={}, errorCode={}, message={}", ex.getStatus(), ex.getErrorCode(), ex.getMessage());
        return ApiResponseEntity.of(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(RuntimeException ex) {
        log.warn("业务状态或参数异常: {}", ex.getMessage());
        return ApiResponseEntity.badRequest(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGlobalException(Exception ex) {
        log.error("系统未处理异常: ", ex);
        return ApiResponseEntity.internalServerError("服务器内部故障，请联系管理员");
    }

}
