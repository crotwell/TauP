package edu.sc.seis.TauP;

import com.google.gson.JsonObject;

import java.io.Serializable;

/**
 * Allows the naming of velocity discontinuities in a model, like moho or cmb.
 */
public class NamedVelocityDiscon implements Cloneable, Serializable {

    public NamedVelocityDiscon(double depth) {
        // unnamed discon
        this.name = null;
        this.depth = depth;
    }

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

    public boolean hasPreferredName() {
        if (preferredName != null) {
            return true;
        }
        return false;
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

    public NamedVelocityDiscon clone() throws CloneNotSupportedException {
        return (NamedVelocityDiscon) super.clone();
    }

    public String getName() {
        return name;
    }

    public double getDepth() {
        return depth;
    }

    public JsonObject asJSON() {
        JsonObject json = new JsonObject();
        json.addProperty(JSONLabels.NAME, getName());
        if (preferredName != null) {
            json.addProperty(JSONLabels.PREFNAME, getName());
        }
        json.addProperty(JSONLabels.DEPTH, (float)getDepth());
        return json;
    }

    protected String name;
    protected String preferredName = null;
    double depth;

    // common names
    public static final String SURFACE = "surface";
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
    public static final String IOCB = "iocb";

    public static final String[] knownDisconNames = {
            SURFACE, ICE, ICEBED, ICECRUST, OCEAN, SEABED, CRUST, MOHO, MANTLE, OUTERCORE, CMB, INNERCORE, ICOCB, IOCB
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
                name.equalsIgnoreCase(NamedVelocityDiscon.IOCB) ||
                name.equalsIgnoreCase(NamedVelocityDiscon.INNERCORE));
    }
}
