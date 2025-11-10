package com.tiki.cart.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleFK(DataIntegrityViolationException ex) {
        String msg = "Sản phẩm không tồn tại hoặc đã bị xoá";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
    }
}
