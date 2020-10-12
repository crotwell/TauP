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
}
