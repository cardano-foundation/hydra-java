package org.cardanofoundation.hydra.core;

public class HydraException extends RuntimeException {

    public HydraException(String message) {
        super(message);
    }

    public HydraException(Exception exception) {
        super(exception);
    }

    public HydraException(String message, Exception e) {
        super(message, e);
    }

    public HydraException(String message, Throwable t) {
        super(message, t);
    }

}
