package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

public class HeadWaveTest {

    TauModel tMod;
    boolean DEBUG = false;

    @BeforeEach
    protected void setUp() throws Exception {
        String modelName = "iasp91";
        tMod = TauModelLoader.load(modelName);
    }
    @Test
    public void pierce() throws TauModelException {
        String phaseName = "PdiffPdiff";
        double receiverDepth = 0;
        SeismicPhase phase = new SeismicPhase(phaseName, tMod, receiverDepth, DEBUG);
        assertEquals(2, phase.headOrDiffractSeq.size());
        List<Arrival> arrivalList = phase.calcTime(210);
        Arrival a = arrivalList.get(0);
        TimeDist[] td = a.getPierce();
        assertEquals(a.getDist(), td[td.length-1].getDistRadian());
        assertEquals(a.getTime(), td[td.length-1].getTime(), 0.000001);
    }

    @Test
    public void pierce_Pn() throws TauModelException {
        String phaseName = "Pn";
        double receiverDepth = 0;
        SeismicPhase phase = new SeismicPhase(phaseName, tMod, receiverDepth, DEBUG);
        assertEquals(1, phase.headOrDiffractSeq.size());
        List<Arrival> arrivalList = phase.calcTime(1);
        Arrival a = arrivalList.get(0);
        TimeDist[] td = a.getPierce();
        assertEquals(a.getDist(), td[td.length-1].getDistRadian());
        assertEquals(a.getTime(), td[td.length-1].getTime(), 0.000001);
    }
}
