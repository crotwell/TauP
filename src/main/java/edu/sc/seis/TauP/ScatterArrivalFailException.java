package edu.sc.seis.TauP;

public class ScatterArrivalFailException extends TauModelException {

    public ScatterArrivalFailException(String message) {
        super(message);
    }

    public ScatterArrivalFailException(Exception t) {
        super(t);
    }

    public ScatterArrivalFailException(String message, Exception t) {
        super(message, t);
    }
}
