package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testSurfaceWave() throws TauModelException {
        String phaseName = "3.0kmps";
        float depth = 23;
        float deg = 30;
            taup.setPhaseNames(new String[] {phaseName});
            taup.setSourceDepth(depth);
            taup.calculate(deg);
            List<Arrival> arrivals = taup.getArrivals();
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
    }

    @Test
    public void testDistKm() throws TauModelException {
        double distKm = 100;
        double phaseVel = 3.2;
        String phaseName = phaseVel+"kmps";
        assertEquals("3.2kmps", phaseName);
        float depth = 0;
        double distDeg = distKm/taup.getTauModel().getRadiusOfEarth()*180.0/Math.PI;
        assertEquals(0.8993, distDeg, 0.0001);
        double longWayDistDeg = 360.0-distDeg;
        double longWayDistKm = longWayDistDeg*taup.getTauModel().getRadiusOfEarth()*Math.PI/180.0;
        taup.setPhaseNames(new String[] {phaseName});
        taup.setSourceDepth(depth);
        taup.calculate(distDeg);
        List<Arrival> arrivals = taup.getArrivals();
        assertEquals(2, arrivals.size(),
                phaseName + " has arrivals for depth " + depth + " at dist " + distDeg);
        assertTrue(arrivals.get(0).getTime() < arrivals.get(1).getTime());
        assertEquals(distKm/phaseVel,
                arrivals.get(0).getTime(),
                0.001f,
                phaseName + " time for depth " + depth + " at dist " + distDeg+"/"+distKm+" "+arrivals.get(0));
        // long way around
        assertEquals(longWayDistKm/phaseVel,
                arrivals.get(1).getTime(),
                0.001f,
                phaseName + " time for depth " + depth + " at dist " + longWayDistDeg+"/"+longWayDistKm+" "+arrivals.get(1));
    }
}
