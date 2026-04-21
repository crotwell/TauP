package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

import java.util.List;

/**
 * Calculatable ray based on index into the phase sampling. Mainly useful for debugging.
 */
public class RayParamIndexRay extends ShootableRay {

    public RayParamIndexRay(int index, GeoDistType geoDistType, Geodesic geodesic) {
        super(geoDistType, geodesic);
        this.index = index;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        Arrival a = phase.createArrivalAtIndex(index);
        a.setSearchValue(this);
        return List.of(a);
    }

    @Override
    public boolean hasDescription() {
        return true;
    }

    @Override
    public String getDescription() {
        return "rp index "+index;
    }

    public int getIndex() {
        return index;
    }

    int index;
}
