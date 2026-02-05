package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeRayTest {

    @Test
    public void timeAtDist() throws TauPException {
        String modelName = "ak135";
        TauModel tMod = TauModelLoader.load(modelName);
        SeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tMod);
        for (double deg = 35;deg <36; deg+=5) {
            List<Arrival> aList = DistanceRay.ofDegrees(deg).calculate(P_phase);
            for (Arrival a : aList) {
                TimeRay timeRay = new TimeRay(a.getTime());
                List<Arrival> taList = timeRay.calculate(P_phase);
                Arrival closest = null;
                for (Arrival ta : taList) {
                    if (closest == null || Math.abs(ta.getDist()-a.getDist())<Math.abs(closest.getDist()-a.getDist())) {
                        closest = ta;
                    }
                }
                String msg = P_phase.getName()+" "+deg+" "+a.getTime();
                assertEquals(a.getTime(), closest.getTime(), 0.01, msg);
                assertEquals(a.getDist(), closest.getDist(), 0.01, msg);
                assertEquals(a.getRayParam(), closest.getRayParam(), 0.03, msg);
                assertEquals(a.getDRayParamDDelta(), closest.getDRayParamDDelta(), 10, msg);

            }
        }
    }
}
