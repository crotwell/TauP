package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.Scatterer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScatterTest {

    @Test
    public void failScatterPhases() throws Exception {
        String[] badScatPhases = {
                "oP",
                "OP",
                "Po",
                "PO",
                "Pedo",
                "PedO",
                "PedoPSoS",
                "SOPop",
                "SOPOP",
                "PedoOp",
                "PedOop"
        };
        double scatterDepth = 100;
        double scatterDistDeg = 2;
        double sourceDepth = 0;
        double receiverDepth = 0;
        TauModel tMod = TauModelLoader.load("iasp91");
        Scatterer scat = new Scatterer(scatterDepth, scatterDistDeg);
        for (String p : badScatPhases) {
            try {
                List<SeismicPhase> scatPhaseList = SeismicPhaseFactory.createSeismicPhases(
                        p,
                        tMod,
                        sourceDepth,
                        receiverDepth,
                        scat,
                        false
                );
                for (SeismicPhase seismicPhase : scatPhaseList) {
                    assertTrue(seismicPhase instanceof FailedSeismicPhase, p);
                }
            } catch (PhaseParseException e) {
                // should throw for bad scat phases that can't be Failed
            }
        }
    }

    @Test
    public void scatterTest_B() throws TauPException {
        String toScatPhase = "Ped";
        String scatToRecPhase = "p";
        double sourceDepth = 0;
        double receiverDepth = 0;
        double scatterDepth = 100;
        double scatterDistDeg = 2;
        double dist = 10;
        Scatterer scat = new Scatterer(scatterDepth, scatterDistDeg);
        doScatterTest(toScatPhase, scatToRecPhase, sourceDepth, receiverDepth, scat, dist);
    }

    @Test
    public void outerCoreScatterTest() throws TauPException {
        String toScatPhase = "PKed";
        String scatToRecPhase = "kp";
        double sourceDepth = 0;
        double receiverDepth = 0;
        double scatterDepth = 3500;
        double scatterDistDeg = 20;
        double dist = 50;
        Scatterer scat = new Scatterer(scatterDepth, scatterDistDeg);
        doScatterTest(toScatPhase, scatToRecPhase, sourceDepth, receiverDepth, scat, dist);
    }

    @Test
    public void ykpinnerCoreScatterTest() throws TauPException {
        String scatToRecPhase = "ykp";
        double scatterDepth = 5500;
        SeismicPhase outboundPhase = SeismicPhaseFactory.createPhase(scatToRecPhase, TauModelLoader.load("iasp91"), scatterDepth, 0, false);
        List<Arrival> outAListIndex = new RayParamRay(0).calculate(outboundPhase);
        assertNotEquals(0, outAListIndex.size());
        assertEquals(0.0, outAListIndex.get(0).getRayParam());
        assertEquals(0.0, outAListIndex.get(0).getDist());
    }

    @Test
    public void innerCoreScatterTest() throws TauPException {
        String toScatPhase = "PKIed";
        String scatToRecPhase = "ykp";
        double sourceDepth = 0;
        double receiverDepth = 0;
        double scatterDepth = 5500;
        double scatterDistDeg = 20;
        double dist = 40;
        Scatterer scat = new Scatterer(scatterDepth, scatterDistDeg);
        doScatterTest(toScatPhase, scatToRecPhase, sourceDepth, receiverDepth, scat, dist);
    }

    public void doScatterTest(String toScatPhase, String scatToRecPhase, double sourceDepth, double receiverDepth, Scatterer scat, double dist) throws TauPException {
        boolean backscatter = false;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhase inboundPhase = SeismicPhaseFactory.createPhase(toScatPhase, tMod, sourceDepth, scat.depth, false);
        Arrival inArr = inboundPhase.getEarliestArrival(scat.dist.getDegrees(tMod.getRadiusOfEarth()));
        assertNotNull(inArr);
        SimpleSeismicPhase outboundPhase = SeismicPhaseFactory.createPhase(scatToRecPhase, tMod, scat.depth, receiverDepth, false);
        Arrival outArr = outboundPhase.getEarliestArrival(dist-scat.dist.getDegrees(tMod.getRadiusOfEarth()));
        assertNotNull(outArr);
        ScatteredSeismicPhase scatPhase = new ScatteredSeismicPhase(inArr, outboundPhase, scat, backscatter);
        List<Arrival> arrList = DistanceRay.ofDegrees(dist).calcScatteredPhase(scatPhase);
        assertNotEquals(0, arrList.size());
        Arrival scatArr = scatPhase.getEarliestArrival(dist);
        assertNotNull(scatArr);
        assertEquals(dist,inArr.getDistDeg()+outArr.getDistDeg());
        assertEquals(inArr.getTime()+outArr.getTime(), scatArr.getTime());
        assertEquals(inArr.getDist()+outArr.getDist(), scatArr.getDist(), 1e-9);
        assertEquals(outArr.getRayParam(), scatArr.getRayParam());
        assertEquals(scat.depth, outArr.getSourceDepth());
        assertEquals(sourceDepth, inArr.getSourceDepth());
        assertEquals(sourceDepth, scatArr.getSourceDepth());
        assertEquals(receiverDepth, outArr.getPhase().getReceiverDepth());
        //
        List<SeismicPhase> scatPhaseList = SeismicPhaseFactory.createSeismicPhases(
                toScatPhase+ PhaseSymbols.SCATTER_CODE+scatToRecPhase,
                tMod,
                sourceDepth,
                receiverDepth,
                scat,
                false
        );
        assertNotEquals(0, scatPhaseList.size());
        assertInstanceOf(ScatteredSeismicPhase.class, scatPhaseList.get(0));
        ScatteredSeismicPhase scatPhaseB = (ScatteredSeismicPhase) scatPhaseList.get(0);
        List<Arrival> arrListB = DistanceRay.ofDegrees(dist).calcScatteredPhase(scatPhaseB);
        assertNotEquals(0, arrListB.size());
        Arrival scatArrB = scatPhaseB.getEarliestArrival(dist);
        assertNotNull(scatArrB);
        assertEquals(dist,inArr.getDistDeg()+outArr.getDistDeg());
        assertEquals(inArr.getTime()+outArr.getTime(), scatArrB.getTime());
        assertEquals(inArr.getDist()+outArr.getDist(), scatArrB.getDist(), 1e-9);
        assertEquals(outArr.getRayParam(), scatArrB.getRayParam());
        assertEquals(scat.depth, outArr.getSourceDepth());
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
    public void isLongWayTest() throws TauPException {
        // PKKP is 234 to 287 deg, so is always long way around
        String modelname = "iasp91";
        double sourceDepth = 0;
        TauP_Time time = new TauP_Time(modelname);
        time.setSingleSourceDepth(sourceDepth);
        time.clearPhaseNames();
        time.appendPhaseName("PKKP");

        List<Arrival> arrivals = time.calcAll(time.calcSeismicPhases(sourceDepth), List.of(DistanceRay.ofDegrees(70)));
        for (Arrival a : arrivals) {
            assertTrue(a.isLongWayAround(), a.toString());
        }
    }

    @Test
    public void pierceScatterPKoKP() throws TauPException {
        String modelname = "iasp91";
        TauP_Pierce pierce = new TauP_Pierce(modelname);
        double sourceDepth = 0;
        pierce.setSingleSourceDepth(sourceDepth);
        Scatterer scat = new Scatterer(3500, 120);
        pierce.setScatterer(scat);
        pierce.clearPhaseNames();
        String phaseName = "PKoKP";
        pierce.appendPhaseName(phaseName);

        DistanceRay dRay = DistanceRay.ofDegrees(360-110);
        ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase) SeismicPhaseFactory.createSeismicPhases(phaseName,
                pierce.getTauModelDepthCorrected(sourceDepth),
                0,
                0,
                scat,
                false).get(0);
        List<Double> arrDistList = dRay.calcRadiansInRange(
                scatPhase.getMinDistance(),
                scatPhase.getMaxDistance(),
                pierce.getRadiusOfEarth(),
                false);
        assertTrue(arrDistList.size()>0);
        for (Double d : arrDistList) {
            System.err.println("   dist in range "+d);
        }
        List<Arrival> dRayArrivalList = dRay.calcScatteredPhase(scatPhase);
        assertEquals(1, dRayArrivalList.size());

        List<Arrival> arrivalsAt250a = pierce.calcAll(pierce.calcSeismicPhases(sourceDepth), List.of(DistanceRay.ofDegrees(360-110)));
        assertEquals(1, arrivalsAt250a.size());


        List<Arrival> arrivals = pierce.calcAll(pierce.calcSeismicPhases(sourceDepth), List.of(DistanceRay.ofDegrees(-110)));
        assertEquals(1, arrivals.size());
        Arrival a_neg110 = arrivals.get(0);
        TimeDist[] p_neg110 = a_neg110.getPierce();


        List<Arrival> arrivalsAt250 = pierce.calcAll(pierce.calcSeismicPhases(sourceDepth), List.of(DistanceRay.ofDegrees(250)));
        assertEquals(1, arrivalsAt250.size());
        Arrival a_250 = arrivalsAt250.get(0);
        TimeDist[] p_250 = a_250.getPierce();
        assertEquals(p_250.length, p_neg110.length);
        int last = p_250.length-1;
        assertEquals(250, p_250[last].getDistDeg(), 1e-9);
        assertEquals(250, p_neg110[last].getDistDeg(), 1e-9);
    }

    @Test
    public void pathBackscatterPedOP() throws TauPException {
        String modelname = "iasp91";
        double sourceDepth = 0;
        TauP_Path path = new TauP_Path(modelname);
        Scatterer scat = new Scatterer(800, -10);
        path.setScatterer(scat);

        path.clearPhaseNames();
        path.appendPhaseName("PedOP");
        List<SeismicPhase> seismicPhaseList = path.calcSeismicPhases(sourceDepth);

        List<Arrival> arrivals = path.calcAll(seismicPhaseList, Arrays.asList(DistanceRay.ofDegrees(35)));
        assertEquals(1, arrivals.size());
        Arrival a_35 = arrivals.get(0);
        ScatteredArrival scatA = (ScatteredArrival) a_35;
        assertEquals(-10, scatA.getScatteredSeismicPhase().getScattererDistanceDeg());
        assertFalse(scatA.isScatterNegativeDirection());
        assertTrue(scatA.isInboundNegativeDirection());
        // path should first go negative to scatterer, then positive to receiver
        List<ArrivalPathSegment> pathSegments = a_35.getPathSegments();
        assertTrue(pathSegments.get(0).getPathPoint(1).getDistDeg()<0);
        assertEquals(-10, pathSegments.get(1).getPathPoint(0).getDistDeg(), 1e-5);
        assertEquals(35.0, pathSegments.get(pathSegments.size()-1).getPathEnd().getDistDeg(), 1e-5);
        assertEquals(35, a_35.getDistDeg(), 1e-5);
    }


    @Test
    public void pathBackscatterPedOPTime() throws TauPException {
        String modelname = "iasp91";
        TauModel tMod = TauModelLoader.load(modelname);
        String phaseName = "PedOP";
        Scatterer scat = new Scatterer(400, DistanceRay.ofFixedHemisphereDegrees(-5));
        List<SeismicPhase> phaseList = SeismicPhaseFactory.createSeismicPhases(phaseName, tMod, 0, 0, scat, false);
        ScatteredSeismicPhase phase = (ScatteredSeismicPhase) phaseList.get(0);
        DistanceRay distRay = DistanceRay.ofDegrees(10);
        List<Arrival> arrivals = distRay.calculate(phase);
        assertTrue(arrivals.size()> 0);
        assertTrue(arrivals.get(0) instanceof ScatteredArrival);
    }
}
