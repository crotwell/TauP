package edu.sc.seis.TauP;

/**
 * Represents a shadow zone within a phase.
 */
public class ShadowZone {

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

    public ShadowZone(SeismicPhase phase,
                      double rayParam,
                      Arrival preArrival,
                      Arrival postArrival) {
        this.rayParam = rayParam;
        this.postArrival = postArrival;
        this.preArrival = preArrival;
    }

    public String toString() {
        return "Shadow zone for ray param "+Outputs.formatRayParam(Math.PI / 180.0 * getRayParam())+" s/deg between \n"
                +Arrival.toStringHeader()+"\n"
                +preArrival.toString()+"\n"
                +postArrival.toString();
    }

    public double getRayParam() {
        return rayParam;
    }

    public Arrival getPostArrival() {
        return postArrival;
    }

    public Arrival getPreArrival() {
        return preArrival;
    }
}
