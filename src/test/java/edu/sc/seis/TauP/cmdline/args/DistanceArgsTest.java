package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.TauP_Time;

import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistanceArgsTest {



    @Test
    public void qmlAndStaxmlTest() throws TauPException {
        TauP_Time time = new TauP_Time();
        MockQmlStaxmlArgs mockQmlStaxml = new MockQmlStaxmlArgs();
        List<Event> eventList = mockQmlStaxml.loadQuakeML();
        assertEquals(1, eventList.size());
        // depth meters
        assertEquals(10000, eventList.get(0).getPreferredOrigin().getDepth().getValue().doubleValue());
        time.getDistanceArgs().qmlStaxmlArgs = mockQmlStaxml;

        time.setPhaseNameList(List.of(PhaseName.parseName("P")));
        assertEquals(1, time.getSourceDepths().size());
        assertEquals(2, time.getReceiverDepths().size()); // SNZO is borehole, 91m
        double srcDepth = time.getSourceDepths().get(0);
        assertEquals(10, srcDepth);
        List<SeismicPhase> phaseList =  time.getSeismicPhases();
        assertEquals(srcDepth, phaseList.get(0).getSourceDepth());
        assertEquals(2, phaseList.size());
        List<RayCalculateable> distList = new ArrayList<>();
        distList.addAll(time.getDistanceArgs().getDistances());
        assertEquals(5, distList.size());
        List<Arrival> arrivalList = time.calcAll(time.getSeismicPhases(), distList);
        // should get arrival for P at CO.JSC, IU.KBS, IU.TUC, not for IU.SNZO
        assertEquals(3, arrivalList.size());
    }
}
