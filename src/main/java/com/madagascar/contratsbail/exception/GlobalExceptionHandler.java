package com.madagascar.contratsbail.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<Map<String, String>> handleIntrouvable(RessourceIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erreur", ex.getMessage()));
    }

    @ExceptionHandler(ValidationMetierException.class)
    public ResponseEntity<Map<String, String>> handleValidationMetier(ValidationMetierException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erreur", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erreurs = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            erreurs.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erreurs);
    }
}
