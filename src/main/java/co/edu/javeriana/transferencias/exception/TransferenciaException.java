package co.edu.javeriana.transferencias.exception;

public class TransferenciaException extends RuntimeException {

    public TransferenciaException(String message) {
        super(message);
    }

    public TransferenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
