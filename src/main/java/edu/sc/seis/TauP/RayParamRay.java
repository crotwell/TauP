package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class RayParamRay extends RayCalculateable {


    public RayParamRay(Double rayParam) {
        this.rayParam = rayParam;
    }

    public static RayParamRay ofRayParam(Double d) {
        RayParamRay ray = new RayParamRay(d);
        return ray;
    }
    public static RayParamRay ofRayParamSDegree(Double d) {
        RayParamRay ray =  RayParamRay.ofRayParam(d/SphericalCoords.dtor);
        return ray;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        List<Arrival> arrivals = new ArrayList<>();
        if (phase.getMinRayParam() <= rayParam && rayParam <= phase.getMaxRayParam()) {
            Arrival phaseArrival = phase.shootRay(rayParam);
            phaseArrival.setShootable(this);
            arrivals.add(phaseArrival);
        }
        return arrivals;
    }

    /**
     * Ray parameter in s/radian.
     */
    Double rayParam;

}
