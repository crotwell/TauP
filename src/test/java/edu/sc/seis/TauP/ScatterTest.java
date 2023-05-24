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
        SimpleSeismicPhase outboundPhase = SeismicPhaseFactory.createPhase(scatToRecPhase, tMod, scatterDepth, receiverDepth, false);
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

        // repeat other depth/dist
        scatterer = 170;
        deg = 170;
        assertEquals(0, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, forwardscatter), 1e-6);
        // backscatter
        assertEquals(360, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, backscatter), 1e-6);

    }


    @Test
    public void calcScatterDistDegPKoKPTest() {
        // --scatter 3500 120
        double scatterer = 120.0;
        double deg = 250.0;
        boolean backscatter = true;
        boolean forwardscatter = false;
        // case A
        assertEquals(130, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, forwardscatter), 1e-6);
        // backscatter
        deg = 10;
        assertEquals(110, ScatteredSeismicPhase.calcScatterDistDeg(deg, scatterer, backscatter), 1e-6);
    }

    @Test
    public void isLongWayTest() throws TauModelException {
        // PKKP is 234 to 287 deg, so is always long way around
        String modelname = "iasp91";
        TauP_Time time = new TauP_Time(modelname);
        time.setSourceDepth(0);
        time.clearPhaseNames();
        time.appendPhaseName("PKKP");
        time.calculate(70);
        List<Arrival> arrivals = time.getArrivals();
        for (Arrival a : arrivals) {
            assertTrue(a.isLongWayAround());
        }
    }

    @Test
    public void pierceScatterPKoKP() throws TauModelException {
        String modelname = "iasp91";
        TauP_Pierce pierce = new TauP_Pierce(modelname);
        pierce.setSourceDepth(0);
        pierce.setScatterer(3500, 120);
        pierce.clearPhaseNames();
        pierce.appendPhaseName("PKoKP");
        pierce.calculate(-110);
        List<Arrival> arrivals = pierce.getArrivals();
        assertEquals(1, arrivals.size());
        Arrival a_neg110 = arrivals.get(0);
        TimeDist[] p_neg110 = a_neg110.getPierce();

        pierce.calculate(250);
        List<Arrival> arrivalsAt250 = pierce.getArrivals();
        assertEquals(1, arrivalsAt250.size());
        Arrival a_250 = arrivalsAt250.get(0);
        TimeDist[] p_250 = a_250.getPierce();
        assertEquals(p_250.length, p_neg110.length);
        int last = p_250.length-1;
        assertEquals(250, p_250[last].getDistDeg(), 1e-9);
        assertEquals(250, p_neg110[last].getDistDeg(), 1e-9);
    }
}
