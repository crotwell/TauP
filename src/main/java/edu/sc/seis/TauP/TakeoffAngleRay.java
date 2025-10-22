package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class TakeoffAngleRay extends ShootableRay {

    public TakeoffAngleRay(Double takeoffAngle) {
        this.takeoffAngle = takeoffAngle;
        setDescription("Takeoff: "+takeoffAngle);
    }

    public static TakeoffAngleRay ofTakeoffAngle(Double d) {
        return new TakeoffAngleRay(d);
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) throws TauPException {
        RayParamRay rayParamRay = forPhase(phase);
        if (rayParamRay == null) {
            return new ArrayList<>();
        }
        List<Arrival> arrivals = rayParamRay.calculate(phase);
        for (Arrival a : arrivals) {
            a.setSearchValue(this);
        }
        return arrivals;
    }

    public RayParamRay forPhase(SeismicPhase phase) {
        double rayParam;
        try {
            if (takeoffAngle > 90 && phase.getInitialPhaseSegment().isDownGoing) {
                return null;
            } else if (takeoffAngle < 90 && ! phase.getInitialPhaseSegment().isDownGoing) {
                return null;
            } else {
                rayParam = phase.calcRayParamForTakeoffAngle(takeoffAngle);
            }
        } catch (NoArrivalException e) {
            return null;
        }
        return new RayParamRay(rayParam);
    }

    public Double getTakeoffAngle() {
        return takeoffAngle;
    }

    Double takeoffAngle;

}
