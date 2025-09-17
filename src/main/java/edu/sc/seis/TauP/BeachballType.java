package edu.sc.seis.TauP;

public enum BeachballType {
    ampp,
    amps,
    ampsv,
    ampsh;

    @Override
    public String toString() {
        switch(this) {
            case ampp: return "P";
            case amps: return "S";
            case ampsv: return "Sv";
            case ampsh: return "Sh";
            default: return name();
        }
    }
}
