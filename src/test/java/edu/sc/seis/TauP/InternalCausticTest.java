package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InternalCausticTest {

    @ParameterizedTest
    @ValueSource(strings={ "SS", "SKSSKS", "SKIKSSKIKS"}) //
    public void testSS(String phasename) throws TauModelException {
        TauModel prem = TauModelLoader.load("prem");
        SeismicPhase phase = SeismicPhaseFactory.createPhase(phasename, prem);
        double mid = (phase.getMaxDistanceDeg()+phase.getMinDistanceDeg())/2;
        ExactDistanceRay edr = DistanceAngleRay.ofExactDegrees(mid);
        List<Arrival> arrivalList = edr.calculate(phase);
        for (Arrival a : arrivalList) {
            assertEquals(1, a.getInternalCausticCount());
        }
    }
}
