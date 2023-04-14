package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ScatterTest {

    @Test
    public void scatterTest() throws TauModelException {
        double sourceDepth = 0;
        double receiverDepth = 0;
        double scatterDepth = 100;
        double scatterDistDeg = 2;
        double dist = 10;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhase inboundPhase = SeismicPhaseFactory.createPhase("Ped", tMod, sourceDepth, scatterDepth, false);
        Arrival inArr = inboundPhase.getEarliestArrival(scatterDistDeg);
        SeismicPhase outboundPhase = SeismicPhaseFactory.createPhase("p", tMod, scatterDepth, receiverDepth, false);
        Arrival outArr = outboundPhase.getEarliestArrival(dist-scatterDistDeg);
        ScatteredSeismicPhase scatPhase = new ScatteredSeismicPhase(inArr, outboundPhase, scatterDepth, scatterDistDeg);
        Arrival scatArr = scatPhase.getEarliestArrival(dist);
        assertEquals(10,inArr.getDistDeg()+outArr.getDistDeg());
        assertEquals(inArr.getTime()+outArr.getTime(), scatArr.getTime());
        assertEquals(inArr.getDist()+outArr.getDist(), scatArr.getDist());
        assertEquals(outArr.getRayParam(), scatArr.getRayParam());
        assertEquals(scatterDepth, outArr.getSourceDepth());
        assertEquals(sourceDepth, inArr.getSourceDepth());
        assertEquals(sourceDepth, scatArr.getSourceDepth());
        assertEquals(receiverDepth, outArr.getPhase().getReceiverDepth());

    }
}
