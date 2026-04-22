package edu.sc.seis.TauP.cmdline.args;

public class ArgumentValidationException extends IllegalArgumentException {
    public ArgumentValidationException() {
    }

    public ArgumentValidationException(String s) {
        super(s);
    }

    public ArgumentValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArgumentValidationException(Throwable cause) {
        super(cause);
    }
}
