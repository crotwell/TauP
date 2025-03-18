package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculatable ray based on ray parameter departing the source in seconds per radian or seconds per degree.
 */
public class RayParamRay extends ShootableRay {

    public RayParamRay(double rayParam) {
        this.rayParam = rayParam;
    }

    public static RayParamRay ofRayParamSRadian(double d) {
        return new RayParamRay(d);
    }
    public static RayParamRay ofRayParamSDegree(double d) {
        return RayParamRay.ofRayParamSRadian(d/SphericalCoords.dtor);
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        if (phase instanceof SimpleSeismicPhase &&
                phase.getMinRayParam() <= rayParam && rayParam <= phase.getMaxRayParam()) {
            Arrival phaseArrival = phase.shootRay(rayParam);
            phaseArrival.setSearchValue(this);
            arrivals.add(phaseArrival);
        }
        return arrivals;
    }

    /**
     * ray param in s/radian
     *
     * @return ray param in seconds per radian
     */
    public Double getRayParam() {
        return rayParam;
    }


    /**
     * Ray parameter in s/radian.
     */
    Double rayParam;
}
