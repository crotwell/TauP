package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class TakeoffAngleRay extends ShootableRay {

    public TakeoffAngleRay(Double takeoffAngle) {
        this.takeoffAngle = takeoffAngle;
    }

    public static TakeoffAngleRay ofTakeoffAngle(Double d) {
        return new TakeoffAngleRay(d);
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        RayParamRay rayParamRay = forPhase(phase);
        if (rayParamRay == null) {
            return new ArrayList<>();
        }
        return rayParamRay.calculate(phase);
    }

    public RayParamRay forPhase(SeismicPhase phase) {
        double rayParam;
        try {
            rayParam = phase.calcRayParamForTakeoffAngle(takeoffAngle);
        } catch (NoArrivalException e) {
            return null;
        }
        return new RayParamRay(rayParam);
    }


    Double takeoffAngle;

}
