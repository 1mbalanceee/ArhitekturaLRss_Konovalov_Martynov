package ru.ssau.jewelry.exception;

public class RecordNotFoundException extends BusinessException {
    public RecordNotFoundException(String message) {
        super(message);
    }
}
