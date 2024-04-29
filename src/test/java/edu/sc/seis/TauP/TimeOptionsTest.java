package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.Scatterer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeOptionsTest {


    @Test
    public void TimeOptions() throws TauPException {
        String modelname = "iasp91";
        TauModel tMod = TauModelLoader.load(modelname);
        TauP_Time timeTool = new TauP_Time();
        timeTool.setPhaseNames(List.of( "P" ));
        timeTool.onlyFirst = true;
        List<Arrival> arrivals = timeTool.calcAll(timeTool.getSeismicPhases(), Collections.singletonList(DistanceRay.ofDegrees(20)));

        assertEquals(1, arrivals.size());
        assertEquals(274.09, arrivals.get(0).getTime(), 0.01);
    }
}
