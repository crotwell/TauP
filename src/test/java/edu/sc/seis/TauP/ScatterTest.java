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
        double dist = 40;
        doScatterTest(toScatPhase, scatToRecPhase, sourceDepth, receiverDepth, scatterDepth, scatterDistDeg, dist);
    }

    public void doScatterTest(String toScatPhase, String scatToRecPhase, double sourceDepth, double receiverDepth, double scatterDepth, double scatterDistDeg, double dist) throws TauModelException {
        boolean backscatter = false;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhase inboundPhase = SeismicPhaseFactory.createPhase(toScatPhase, tMod, sourceDepth, scatterDepth, false);
        Arrival inArr = inboundPhase.getEarliestArrival(scatterDistDeg);
        assertNotNull(inArr);
        SeismicPhase outboundPhase = SeismicPhaseFactory.createPhase(scatToRecPhase, tMod, scatterDepth, receiverDepth, false);
        Arrival outArr = outboundPhase.getEarliestArrival(dist-scatterDistDeg);
        assertNotNull(outArr);
        ScatteredSeismicPhase scatPhase = new ScatteredSeismicPhase(inArr, outboundPhase, scatterDepth, scatterDistDeg, backscatter);
        List<Arrival> arrList = scatPhase.calcTime(dist);
        assertNotEquals(0, arrList.size());
        Arrival scatArr = scatPhase.getEarliestArrival(dist);
        assertNotNull(scatArr);
        assertEquals(dist,inArr.getDistDeg()+outArr.getDistDeg());
        assertEquals(inArr.getTime()+outArr.getTime(), scatArr.getTime());
        assertEquals(inArr.getDist()+outArr.getDist(), scatArr.getDist(), 1e-9);
        assertEquals(outArr.getRayParam(), scatArr.getRayParam());
        assertEquals(scatterDepth, outArr.getSourceDepth());
        assertEquals(sourceDepth, inArr.getSourceDepth());
        assertEquals(sourceDepth, scatArr.getSourceDepth());
        assertEquals(receiverDepth, outArr.getPhase().getReceiverDepth());
        //
        List<SeismicPhase> scatPhaseList = SeismicPhaseFactory.createSeismicPhases(
                toScatPhase+LegPuller.SCATTER_CODE+scatToRecPhase,
                tMod,
                sourceDepth,
                receiverDepth,
                scatterDepth,
                scatterDistDeg,
                false
        );
        assertNotEquals(0, scatPhaseList.size());
        assertInstanceOf(ScatteredSeismicPhase.class, scatPhaseList.get(0));
        ScatteredSeismicPhase scatPhaseB = (ScatteredSeismicPhase) scatPhaseList.get(0);
        List<Arrival> arrListB = scatPhaseB.calcTime(dist);
        assertNotEquals(0, arrListB.size());
        Arrival scatArrB = scatPhaseB.getEarliestArrival(dist);
        assertNotNull(scatArrB);
        assertEquals(dist,inArr.getDistDeg()+outArr.getDistDeg());
        assertEquals(inArr.getTime()+outArr.getTime(), scatArrB.getTime());
        assertEquals(inArr.getDist()+outArr.getDist(), scatArrB.getDist(), 1e-9);
        assertEquals(outArr.getRayParam(), scatArrB.getRayParam());
        assertEquals(scatterDepth, outArr.getSourceDepth());
        assertEquals(sourceDepth, inArr.getSourceDepth());
        assertEquals(sourceDepth, scatArrB.getSourceDepth());
        assertEquals(receiverDepth, outArr.getPhase().getReceiverDepth());
    }

    @Test
    public void calcScatterDistDegTest() {
        // case a
        double scatterer = 5.0;
        double deg = 15;
        boolean backscatter = true;
        boolean forwardscatter = false;
        // case A
        assertEquals(10, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, forwardscatter), 1e-6);
        // backscatter
        assertEquals(350, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, backscatter), 1e-6);
        // case B
        scatterer = 20;
        assertEquals(355, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, forwardscatter), 1e-6);
        // backscatter
        assertEquals(5, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, backscatter), 1e-6);
        // case C
        scatterer = -5;
        assertEquals(340, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, forwardscatter), 1e-6);
        // backscatter
        assertEquals(20, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, backscatter), 1e-6);
    }
}
