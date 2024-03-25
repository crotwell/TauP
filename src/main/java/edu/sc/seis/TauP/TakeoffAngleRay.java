package edu.sc.seis.TauP;

import java.util.List;

public class TakeoffAngleRay extends RayCalculateable {

    public TakeoffAngleRay(Double takeoffAngle) {
        this.takeoffAngle = takeoffAngle;
    }

    public static TakeoffAngleRay ofTakeoffAngle(Double d) {
        TakeoffAngleRay takeoffRay = new TakeoffAngleRay(d);
        return takeoffRay;
    }


    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws SlownessModelException, NoSuchLayerException {
        return forPhase(phase).calculate(phase);
    }

    public RayParamRay forPhase(SeismicPhase phase) {
        Double rayParam = phase.calcRayParamForTakeoffAngle(takeoffAngle);
        RayParamRay ray = new RayParamRay(rayParam);
        return ray;
    }
    Double takeoffAngle;
}
