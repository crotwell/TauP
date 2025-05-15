package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.sc.seis.TauP.cmdline.TauP_Time;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.List;


public class SurfaceWaveTest {

    @BeforeEach
    protected void setUp() throws Exception {
        taup = new TauP_Time("ak135");
    }

    TauP_Time taup;


    @Test
    public void testSurfaceWave() throws TauPException {
        String phaseName = "3.0kmps";
        float depth = 23;
        float deg = 30;
        taup.setPhaseNames(List.of(phaseName));
        taup.setSingleSourceDepth(depth);


        List<SeismicPhase> phaseList = taup.calcSeismicPhases(depth);
        List<RayCalculateable> distanceValues = List.of(DistanceRay.ofDegrees(deg));
        List<Arrival> arrivals = taup.calcAll(phaseList, distanceValues);
        assertTrue(arrivals.size() > 0,
                   phaseName + " has arrivals for depth " + depth + " at dist " + deg);
        assertEquals(1111.95f,
                     arrivals.get(0).getTime(),
                     0.07f,
                     phaseName + " time for depth " + depth + " at dist " + deg);
        // long way around
        assertEquals(12231.44f,
                     arrivals.get(1).getTime(),
                     0.07f,
                     phaseName + " time for depth " + depth + " at dist " + deg);
        // pierce for surface wave should be just start and finish
        TimeDist[] pierce = arrivals.get(0).getPierce();
        assertEquals(0, pierce[0].getDistRadian());
        assertEquals(0, pierce[0].getDepth());
        assertEquals(0, pierce[0].getTime());
        assertEquals(2, pierce.length);
    }

    @Test
    public void testDistKm() throws TauPException {
        double distKm = 100;
        double phaseVel = 3.2;
        String phaseName = phaseVel+"kmps";
        assertEquals("3.2kmps", phaseName);
        float sourceDepth = 0;
        double distDeg = distKm/taup.getTauModelDepthCorrected(sourceDepth).getRadiusOfEarth()*180.0/Math.PI;
        assertEquals(0.8993, distDeg, 0.0001);
        double longWayDistDeg = 360.0-distDeg;
        double longWayDistKm = longWayDistDeg*taup.getTauModelDepthCorrected(sourceDepth).getRadiusOfEarth()*Math.PI/180.0;
        taup.setPhaseNames(List.of(phaseName));
        taup.setSingleSourceDepth(sourceDepth);
        List<SeismicPhase> phaseList = taup.calcSeismicPhases(sourceDepth);
        List<RayCalculateable> distanceValues = List.of(DistanceRay.ofDegrees(distDeg));
        List<Arrival> arrivals = taup.calcAll(phaseList, distanceValues);
        assertEquals(2, arrivals.size(),
                phaseName + " has arrivals for depth " + sourceDepth + " at dist " + distDeg);
        assertTrue(arrivals.get(0).getTime() < arrivals.get(1).getTime());
        assertEquals(distKm/phaseVel,
                arrivals.get(0).getTime(),
                0.001f,
                phaseName + " time for depth " + sourceDepth + " at dist " + distDeg+"/"+distKm+" "+arrivals.get(0));
        // long way around
        assertEquals(longWayDistKm/phaseVel,
                arrivals.get(1).getTime(),
                0.001f,
                phaseName + " time for depth " + sourceDepth + " at dist " + longWayDistDeg+"/"+longWayDistKm+" "+arrivals.get(1));
    }
}
