package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TauP_TimeTest {

    @Test
    public void testOnlyFirst() throws TauPException, IOException {
        TauP_Time time = new TauP_Time();
        time.onlyFirst=true;
        time.getDistanceArgs().setDegreeList(List.of(5.0, 10.0, 15.0));
        time.getPhaseArgs().setPhaseNames(List.of("P"));
        time.init();
        List<RayCalculateable> rays = time.getDistanceArgs().getRayCalculatables( time.getSourceArgs());
        List<Arrival> arrivals = time.calcAll(time.getSeismicPhases(), rays);
        assertEquals(arrivals.size(), rays.size());
    }

    @Test
    public void hilbertSymbol() {
        assertEquals(Math.PI/4, SeismicPhaseReflTransHolder.normPhaseRadian(Math.PI/4), 0.0001);
        assertEquals("1", ArrivalAmplitude.getHilbertSymbol(-1*Math.PI/8+0.001));
        assertEquals("-h", ArrivalAmplitude.getHilbertSymbol(-1*Math.PI/8-0.001));
        assertEquals("1", ArrivalAmplitude.getHilbertSymbol(0));
        assertEquals("1", ArrivalAmplitude.getHilbertSymbol(Math.PI/8-0.001));
        assertEquals("h", ArrivalAmplitude.getHilbertSymbol(Math.PI/8));
        assertEquals("h", ArrivalAmplitude.getHilbertSymbol(Math.PI/4));
        assertEquals("H", ArrivalAmplitude.getHilbertSymbol(Math.PI/2-0.00001));
        assertEquals("h", ArrivalAmplitude.getHilbertSymbol(3*Math.PI/4-0.00001));
        assertEquals("h", ArrivalAmplitude.getHilbertSymbol(3*Math.PI/4+0.00001));
        assertEquals("-1", ArrivalAmplitude.getHilbertSymbol(Math.PI-0.00001));
        assertEquals("-1", ArrivalAmplitude.getHilbertSymbol(Math.PI+0.00001));
        assertEquals("-h", ArrivalAmplitude.getHilbertSymbol(-3*Math.PI/4-0.00001));
        assertEquals("-H", ArrivalAmplitude.getHilbertSymbol(-1*Math.PI/2));
    }
}
