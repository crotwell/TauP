package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

public class TakeoffAngleRay extends ShootableRay {

    public TakeoffAngleRay(Double takeoffAngle) {
        this.takeoffAngle = takeoffAngle;
    }

    public TakeoffAngleRay(TakeoffAngleRay dr) {
        this.takeoffAngle = dr.takeoffAngle;
    }

    public static TakeoffAngleRay ofTakeoffAngle(Double d) {
        TakeoffAngleRay takeoffRay = new TakeoffAngleRay(d);
        return takeoffRay;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        RayParamRay rayParamRay = forPhase(phase);
        if (rayParamRay == null) {
            return new ArrayList<Arrival>();
        }
        return rayParamRay.calculate(phase);
    }

    public RayParamRay forPhase(SeismicPhase phase) {
        Double rayParam = null;
        try {
            rayParam = phase.calcRayParamForTakeoffAngle(takeoffAngle);
        } catch (NoArrivalException e) {
            return null;
        }
        RayParamRay ray = new RayParamRay(rayParam);
        return ray;
    }


    Double takeoffAngle;

}
