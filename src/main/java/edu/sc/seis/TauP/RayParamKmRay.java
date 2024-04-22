package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

import java.util.List;

public class RayParamKmRay extends ShootableRay {

    public RayParamKmRay(Double rpSecKm) {
        this.rpSecKm = rpSecKm;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        RayParamRay rpRay = RayParamRay.ofRayParam(getRayParamSKm()*phase.getTauModel().getRadiusOfEarth());
        return rpRay.calculate(phase);
    }

    /**
     * ray param in s/km
     *
     * @return
     */
    public Double getRayParamSKm() {
        return rpSecKm;
    }

    private final double rpSecKm;
}
