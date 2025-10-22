package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

public class IncidentAngleRay extends ShootableRay {

    public IncidentAngleRay(Double incidentAngle) {
        this.incidentAngle = incidentAngle;
        setDescription("Incident: "+ incidentAngle);
    }

    public static IncidentAngleRay ofIncidentAngle(Double d) {
        return new IncidentAngleRay(d);
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
            if (incidentAngle < 90 && phase.getFinalPhaseSegment().isDownGoing) {
                return null;
            } else if (incidentAngle > 90 && ! phase.getFinalPhaseSegment().isDownGoing) {
                return null;
            } else {
                rayParam = phase.calcRayParamForIncidentAngle(incidentAngle);
            }
        } catch (NoArrivalException e) {
            return null;
        }
        return new RayParamRay(rayParam);
    }

    public Double getIncidentAngle() {
        return incidentAngle;
    }

    Double incidentAngle;

}
