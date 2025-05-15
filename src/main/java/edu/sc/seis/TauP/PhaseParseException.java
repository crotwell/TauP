package edu.sc.seis.TauP;

public class PhaseParseException extends TauModelException {

    public PhaseParseException(String message, String phasename, int offset) {
        super(message);
        this.phasename = phasename;
        this.offset = offset;
    }

    public PhaseParseException(Exception t) {
        super(t);
    }

    public PhaseParseException(String message, Exception t) {
        super(message, t);
    }

    String phasename;
    int offset;
}
