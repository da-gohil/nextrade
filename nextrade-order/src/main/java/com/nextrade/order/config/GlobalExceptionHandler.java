package com.nextrade.order.config;

import com.nextrade.common.dto.ApiResponse;
import com.nextrade.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex, WebRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.<Void>builder()
                .status(ex.getStatus().value()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                .status(400).message("Validation failed").errors(errors).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.<Void>builder()
                .status(500).message("Internal server error").build());
    }
}
