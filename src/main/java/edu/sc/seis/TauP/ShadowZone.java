package edu.sc.seis.TauP;

import com.google.gson.JsonObject;

/**
 * Represents a shadow zone within a phase.
 */
public class ShadowZone {

    private final TauBranch hszBranch;

    double rayParam;

    /**
     * The Arrival for the ray that arrives after the shadow zone. For example the first ray that penetrates and turns
     * below a low velocity zone.
     */
    Arrival postArrival;

    /**
     * The ray that arrives before/above the shadow zone. For example last ray to turn above a
     * low velocity zone.
     */
    Arrival preArrival;

    boolean isPWave;

    public ShadowZone(double rayParam, TauBranch hszBranch) {
        this.rayParam = rayParam;
        this.hszBranch = hszBranch;
    }

    public void setPrePostArrival(Arrival preArrival,
                                  Arrival postArrival) {
        this.postArrival = postArrival;
        this.preArrival = preArrival;
    }

    public String toString() {
        return preArrival.getName()+" Shadow zone for ray param "+Outputs.formatRayParam(Math.PI / 180.0 * getRayParam())+" s/deg "
                +" depths "+hszBranch.getTopDepth()+" to "+hszBranch.getBotDepth()+",  between arrivals:\n"
                +"  "+Arrival.toStringHeader()+"\n"
                +"  "+preArrival.toString()+"\n"
                +"  "+postArrival.toString();
    }

    public double getRayParam() {
        return rayParam;
    }

    public double getTopDepth() {
        return hszBranch.getTopDepth();
    }

    public double getBotDepth() {
        return hszBranch.getBotDepth();
    }

    public Arrival getPostArrival() {
        return postArrival;
    }

    public Arrival getPreArrival() {
        return preArrival;
    }
}
