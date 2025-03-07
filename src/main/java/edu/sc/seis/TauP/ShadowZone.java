package edu.sc.seis.TauP;

/**
 * Represents a shadow zone within a phase.
 */
public class ShadowZone {

    double rayParam;

    int rayParamIndex;

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
                      int rayParamIndex,
                      Arrival preArrival,
                      Arrival postArrival) {
        this.rayParam = rayParam;
        this.rayParamIndex = rayParamIndex;
        this.postArrival = postArrival;
        this.preArrival = preArrival;
    }
}
