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
        List<Arrival> arrivals = time.calcAll(time.getSeismicPhases(), time.getDistanceArgs().getRayCalculatables());
        assertEquals(arrivals.size(), time.getDistanceArgs().getRayCalculatables().size());
    }
}
