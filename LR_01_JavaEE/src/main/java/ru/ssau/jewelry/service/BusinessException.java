package ru.ssau.jewelry.service;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
