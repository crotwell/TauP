package edu.sc.seis.TauP;

import java.util.List;

public class RayParamKmRay extends ShootableRay {

    public RayParamKmRay(Double rpSecKm) {
        this.rpSecKm = rpSecKm;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        RayParamRay rpRay = RayParamRay.ofRayParamSRadian(getRayParamSKm()*phase.getTauModel().getRadiusOfEarth());
        return rpRay.calculate(phase);
    }

    /**
     * ray param in s/km
     *
     * @return ray param in seconds per kilometer
     */
    public Double getRayParamSKm() {
        return rpSecKm;
    }

    private final double rpSecKm;
}
