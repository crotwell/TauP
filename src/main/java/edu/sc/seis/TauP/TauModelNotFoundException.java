package edu.sc.seis.TauP;

public class TauModelNotFoundException extends TauModelException {
    public TauModelNotFoundException(String message) {
        super(message);
    }

    public TauModelNotFoundException(Exception t) {
        super(t);
    }

    public TauModelNotFoundException(String message, Exception t) {
        super(message, t);
    }
}
