package edu.sc.seis.TauP;

import java.io.Serializable;

public class NamedVelocityDiscon implements Cloneable, Serializable {

    public NamedVelocityDiscon(String name, double depth) {
        if (name == null ) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.name = name;
        this.depth = depth;
        if (name.equalsIgnoreCase(MANTLE)) { this.preferredName = MOHO;}
        if (name.equalsIgnoreCase(OUTERCORE)) { this.preferredName = CMB;}
        if (name.equalsIgnoreCase(INNERCORE)) { this.preferredName = ICOCB;}
    }

    public String getPreferredName() {
        if (preferredName != null) {
            return preferredName;
        }
        return name;
    }

    public boolean isMoho() {
        return isMoho(getPreferredName());
    }

    public boolean isCmb() {
        return isCmb(getPreferredName());
    }

    public boolean isIocb() {
        return isIcocb(getPreferredName());
    }

    public String toString() {
        String pf_name = "";
        if (preferredName != null) {
            pf_name = " (" + this.preferredName + ") " ;
        }
        return this.name + pf_name + this.depth;
    }

    String name;
    String preferredName = null;
    double depth;

    // common names
    public static final String ICE = "ice";
    public static final String ICEBED = "ice-ocean";
    public static final String ICECRUST = "ice-crust";
    public static final String OCEAN = "ocean";
    public static final String SEABED = "seabed";
    public static final String CRUST = "crust";
    public static final String MOHO = "moho";
    public static final String MANTLE = "mantle";
    public static final String OUTERCORE = "outer-core";
    public static final String CMB = "cmb";
    public static final String INNERCORE = "inner-core";
    public static final String ICOCB = "icocb";

    public static final String[] knownDisconNames = {
            ICE, ICEBED, OCEAN, SEABED, CRUST, MOHO, MANTLE, OUTERCORE, CMB, INNERCORE, ICOCB
    };

    public static boolean isIceBed(String name) {
        return name != null && (name.equalsIgnoreCase(NamedVelocityDiscon.ICEBED) ||
                name.equalsIgnoreCase(NamedVelocityDiscon.ICECRUST));
    }

    public static boolean isSeabed(String name) {
        return name != null && ( name.equalsIgnoreCase(NamedVelocityDiscon.SEABED) ||
                name.equalsIgnoreCase(NamedVelocityDiscon.CRUST));
    }

    public static boolean isMoho(String name) {
        return name != null && ( name.equalsIgnoreCase(NamedVelocityDiscon.MANTLE) ||
                name.equalsIgnoreCase(NamedVelocityDiscon.MOHO));
    }

    public static boolean isCmb(String name) {
        return name != null && ( name.equalsIgnoreCase(NamedVelocityDiscon.CMB) ||
                name.equalsIgnoreCase(NamedVelocityDiscon.OUTERCORE));
    }

    public static boolean isIcocb(String name) {
        return name != null && ( name.equalsIgnoreCase(NamedVelocityDiscon.ICOCB) ||
                name.equalsIgnoreCase(NamedVelocityDiscon.INNERCORE));
    }
}
