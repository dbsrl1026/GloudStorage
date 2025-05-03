package com.gloud.document.exception;


import com.gloud.document.dto.ErrorResponse;
import com.gloud.document.util.ErrorResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;


@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final ErrorResponseUtil errorResponseUtil;


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error(ex.getMessage(), ex);
        return errorResponseUtil.createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error(ex.getMessage(), ex);
        return errorResponseUtil.createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        log.error(ex.getMessage(), ex);
        return errorResponseUtil.createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.error(ex.getMessage(), "파일 크기가 너무 큽니다. 개당 10MB, 총합 100MB를 초과해서는 안됩니다.");
        return errorResponseUtil.createErrorResponse(HttpStatus.BAD_REQUEST, "파일 크기가 너무 큽니다. 개당 10MB, 총합 100MB를 초과해서는 안됩니다.");
    }

    @ExceptionHandler(DuplicateDirectoryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDirectoryException(DuplicateDirectoryException ex) {
        log.error(ex.getMessage(), ex);
        return errorResponseUtil.createErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        log.error(ex.getMessage(), ex);
        return errorResponseUtil.createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 모든 그 외 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return errorResponseUtil.createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error.");
    }


}
