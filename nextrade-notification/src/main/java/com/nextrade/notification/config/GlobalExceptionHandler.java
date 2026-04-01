package com.nextrade.notification.config;

import com.nextrade.common.dto.ApiResponse;
import com.nextrade.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.<Void>builder()
                .status(ex.getStatus().value()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.<Void>builder()
                .status(500).message("Internal server error").build());
    }
}
