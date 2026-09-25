package com.madagascar.contratsbail.exception;

/** Exception levee lorsqu'une regle metier (non couverte par les annotations de validation) est violee. */
public class ValidationMetierException extends RuntimeException {
    public ValidationMetierException(String message) {
        super(message);
    }
}
