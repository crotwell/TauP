package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

public class RayParamRay extends ShootableRay {

    public RayParamRay(RayParamRay rpr) {
        this.rayParam = rpr.getRayParam();
    }

    public RayParamRay(double rayParam) {
        this.rayParam = rayParam;
    }

    public static RayParamRay ofRayParam(double d) {
        RayParamRay ray = new RayParamRay(d);
        return ray;
    }
    public static RayParamRay ofRayParamSDegree(double d) {
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
     * ray param in s/radian
     *
     * @return
     */
    public Double getRayParam() {
        return rayParam;
    }


    /**
     * Ray parameter in s/radian.
     */
    Double rayParam;
}
