package edu.sc.seis.TauP;

import java.util.List;

public abstract class RayCalculateable {

    public abstract List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException;

    public abstract boolean isLatLonable();
    public abstract LatLonable getLatLonable();

}
