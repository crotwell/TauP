package edu.sc.seis.TauP;


import java.util.List;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

public class SurfaceWaveTest  extends TestCase {

    @Before
    @Test
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
            assertTrue(phaseName + " has arrivals for depth " + depth + " at dist " + deg,
                       arrivals.size() > 0);
            assertEquals(phaseName + " time for depth " + depth + " at dist " + deg,
                         1111.95f,
                         arrivals.get(0).getTime(),
                         0.07f);
            // long way around
            assertEquals(phaseName + " time for depth " + depth + " at dist " + deg,
                         12231.44f,
                         arrivals.get(1).getTime(),
                         0.07f);
    }
}
