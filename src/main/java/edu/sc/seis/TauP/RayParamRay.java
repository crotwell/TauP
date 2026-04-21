package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculatable ray based on ray parameter departing the source in seconds per radian or seconds per degree.
 */
public class RayParamRay extends ShootableRay {

    public RayParamRay(double rayParam, DistanceCalc distCalc) {
        super(distCalc);
        this.rayParam = rayParam;
    }

    public static RayParamRay ofRayParamSRadian(double d, DistanceCalc distCalc) {
        RayParamRay rp = new RayParamRay(d, distCalc);
        rp.setDescription(d+" s/rad");
        return rp;
    }
    public static RayParamRay ofRayParamSDegree(double d, DistanceCalc distCalc) {
        RayParamRay rp =  RayParamRay.ofRayParamSRadian(d/SphericalCoords.dtor, distCalc);
        rp.rayParamSDeg = d;
        rp.setDescription(d+" s/deg");
        return rp;
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

    public boolean hasSDegree() {
        return rayParamSDeg != null;
    }
    /**
     * ray param in s/radian
     *
     * @return ray param in seconds per radian
     */
    public Double getRayParamSDegree() {
        if (rayParamSDeg != null) {
            return rayParamSDeg;
        }
        return rayParam/SphericalCoords.RtoD;
    }

    /**
     * Ray parameter in s/radian.
     */
    Double rayParam;
    /**
     * Optional value in s/deg if constructed via ofRayParamSDegree().
     */
    Double rayParamSDeg = null;
}
