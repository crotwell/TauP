package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.TauP_Time;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistanceArgsTest {



    @Test
    public void qmlAndStaxmlTest() throws TauPException {
        TauP_Time time = new TauP_Time();
        time.getDistanceArgs().qmlStaxmlArgs = new MockQmlStaxmlArgs();
        time.setPhaseNameList(List.of(new PhaseName("P")));
        List<SeismicPhase> phaseList =  time.getSeismicPhases();
        for (SeismicPhase seismicPhase : phaseList) {
            System.err.println("in test: "+seismicPhase.describeShort());
        }
        assertEquals(4, phaseList.size());
        List<RayCalculateable> distList = new ArrayList<>();
        distList.addAll(time.getDistanceArgs().getDistances());
        for (RayCalculateable rayCalc : distList) {
            System.err.println("ray: "+rayCalc);
        }
        List<Arrival> arrivalList = time.calcAll(time.getSeismicPhases(), distList);
        // should get arrival for P at CO.JSC, IU.KBS, IU.TUC, not for IU.SNZO
        assertEquals(3, arrivalList.size());
    }
}
