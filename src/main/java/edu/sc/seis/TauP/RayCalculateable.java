package edu.sc.seis.TauP;

import java.util.List;

public abstract class RayCalculateable {

    public abstract List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException;

    public LatLonable getLatLonable() {
        return latLonable;
    }

    public void setLatLonable(LatLonable latLonable) {
        if (this.latLonable != null ) {
            throw new RuntimeException("Cannot set latlonable after already set");
        }
        this.latLonable = latLonable;
    }
    LatLonable latLonable = null;
}
