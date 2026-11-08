package org.mromichov.pocket_budget.exceptions;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        Throwable current = exception;

        while (current.getCause() != null) {
            current = current.getCause();
            if (current instanceof ConstraintViolationException && "users_username_key".equals(((ConstraintViolationException) current).getConstraintName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Имя пользователя уже занято");
            }
        }
        throw exception;
    }
}
