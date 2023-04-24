package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScatterTest {

    @Test
    public void scatterTest_B() throws TauModelException {
        String toScatPhase = "Ped";
        String scatToRecPhase = "p";
        double sourceDepth = 0;
        double receiverDepth = 0;
        double scatterDepth = 100;
        double scatterDistDeg = 2;
        double dist = 10;
        doScatterTest(toScatPhase, scatToRecPhase, sourceDepth, receiverDepth, scatterDepth, scatterDistDeg, dist);
    }

    @Test
    public void outerCoreScatterTest() throws TauModelException {
        String toScatPhase = "PKed";
        String scatToRecPhase = "kp";
        double sourceDepth = 0;
        double receiverDepth = 0;
        double scatterDepth = 3500;
        double scatterDistDeg = 20;
        double dist = 50;
        doScatterTest(toScatPhase, scatToRecPhase, sourceDepth, receiverDepth, scatterDepth, scatterDistDeg, dist);
    }

    @Test
    public void innerCoreScatterTest() throws TauModelException {
        String toScatPhase = "PKIed";
        String scatToRecPhase = "ykp";
        double sourceDepth = 0;
        double receiverDepth = 0;
        double scatterDepth = 5500;
        double scatterDistDeg = 20;
        double dist = 50;
        doScatterTest(toScatPhase, scatToRecPhase, sourceDepth, receiverDepth, scatterDepth, scatterDistDeg, dist);
    }

    public void doScatterTest(String toScatPhase, String scatToRecPhase, double sourceDepth, double receiverDepth, double scatterDepth, double scatterDistDeg, double dist) throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhase inboundPhase = SeismicPhaseFactory.createPhase(toScatPhase, tMod, sourceDepth, scatterDepth, false);
        Arrival inArr = inboundPhase.getEarliestArrival(scatterDistDeg);
        assertNotNull(inArr);
        SeismicPhase outboundPhase = SeismicPhaseFactory.createPhase(scatToRecPhase, tMod, scatterDepth, receiverDepth, false);
        Arrival outArr = outboundPhase.getEarliestArrival(dist-scatterDistDeg);
        assertNotNull(outArr);
        ScatteredSeismicPhase scatPhase = new ScatteredSeismicPhase(inArr, outboundPhase, scatterDepth, scatterDistDeg);
        Arrival scatArr = scatPhase.getEarliestArrival(dist);
        assertNotNull(scatArr);
        assertEquals(dist,inArr.getDistDeg()+outArr.getDistDeg());
        assertEquals(inArr.getTime()+outArr.getTime(), scatArr.getTime());
        assertEquals(inArr.getDist()+outArr.getDist(), scatArr.getDist());
        assertEquals(outArr.getRayParam(), scatArr.getRayParam());
        assertEquals(scatterDepth, outArr.getSourceDepth());
        assertEquals(sourceDepth, inArr.getSourceDepth());
        assertEquals(sourceDepth, scatArr.getSourceDepth());
        assertEquals(receiverDepth, outArr.getPhase().getReceiverDepth());

    }
}
