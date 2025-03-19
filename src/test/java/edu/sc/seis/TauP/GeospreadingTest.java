package edu.sc.seis.TauP;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeospreadingTest {

    @Test
    public void testGeoSpread() throws Exception {
        TauModel tMod = TauModelLoader.load("ak135fcont");
        SeismicPhase Pphase = SeismicPhaseFactory.createPhase("P", tMod);
        for (int i = 10; i < 180; i++) {
            List<Arrival> arrivalList = DistanceRay.ofDegrees(i).calculate(Pphase);
            for (Arrival a : arrivalList) {
                assertEquals(geospread(a), a.getAmplitudeGeometricSpreadingFactor(), 0.000001, a.getCommentLine());

            }
        }
    }

    public double geospread(Arrival a) throws TauModelException {
        double out = 1;
        TauModel tMod = a.getTauModel();
        double R = tMod.radiusOfEarth;
        VelocityModel vMod = tMod.getVelocityModel();
        out *= a.getPhase().velocityAtSource()/
                ((R-a.getReceiverDepth())*(R-a.getReceiverDepth())*(R-a.getSourceDepth()));
        out *= Math.tan(a.getTakeoffAngleRadian())/Math.cos(a.getIncidentAngleRadian());
        out *= 1/Math.sin(a.getModuloDist());
        double dRPdDist = a.getDRayParamDDelta(); // dp/ddelta = dT/ddelta
        out *= Math.abs(dRPdDist);
        return Math.sqrt(out);
    }
}
