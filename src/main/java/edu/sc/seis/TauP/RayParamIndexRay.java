package edu.sc.seis.TauP;

import java.util.List;

public class RayParamIndexRay extends ShootableRay {

    public RayParamIndexRay(int index) {
        this.index = index;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        Arrival a = phase.createArrivalAtIndex(index);
        a.setSearchValue(this);
        return List.of(a);
    }

    int index;
}
