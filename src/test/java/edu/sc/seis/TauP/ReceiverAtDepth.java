package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.PhaseSymbols.isExclusiveDowngoingSymbol;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;


public class ReceiverAtDepth {

    // @ Test
    public void test() throws TauModelException {
        double depthStep = 30;
        
        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName);
        for (double depth = 0; depth < 2000; depth += depthStep) {
            TauModel tModDepth = tMod.depthCorrect(depth);
            for (double recDepth = 0; recDepth < 2000; recDepth+=depthStep) {
                TauModel tModRecDepth = tModDepth.splitBranch(recDepth);
                TauModel flippedMod = tMod.depthCorrect(recDepth);
                flippedMod = flippedMod.splitBranch(depth);
                System.out.println("Check P source="+depth+" receiver="+recDepth);
                check(tModRecDepth, flippedMod, "P");
                System.out.println("Check S source="+depth+" receiver="+recDepth);
                check(tModRecDepth, flippedMod, "S");
            }
        }
    }

    void check(TauModel tMod, TauModel tModRec, String phaseName) throws TauModelException {
        double distStep = 11;
        check(tMod, tModRec, phaseName, distStep);
    }
    
    void check(TauModel tMod, TauModel tModRec, String phaseName, double distStep) throws TauModelException {
        double receiverDepth = tModRec.getSourceDepth();
        String reversedName = new StringBuilder(phaseName).reverse().toString();
        SeismicPhase phase;
        SeismicPhase upPhase;
        SeismicPhase endsDowngoingPhase = null;
        SeismicPhase flippedPhase;
        SeismicPhase upFlippedPhase;
        SeismicPhase endsDowngoingFlippedPhase = null;
        if (tMod.getSourceDepth() == receiverDepth) {
            phase = SeismicPhaseFactory.createPhase(phaseName, tMod, tMod.getSourceDepth(), receiverDepth);
            upPhase = null;
            endsDowngoingPhase = null;
            flippedPhase = SeismicPhaseFactory.createPhase(phaseName, tModRec, tModRec.getSourceDepth(), tMod.getSourceDepth());
            upFlippedPhase = null;
            endsDowngoingFlippedPhase = null;
        } else if (tMod.getSourceDepth() > receiverDepth) {
            phase = SeismicPhaseFactory.createPhase(phaseName.toUpperCase(), tMod, tMod.getSourceDepth(), receiverDepth);
            upPhase = SeismicPhaseFactory.createPhase(phaseName.toLowerCase(), tMod, tMod.getSourceDepth(), receiverDepth);
            endsDowngoingPhase = null;
            flippedPhase = SeismicPhaseFactory.createPhase(reversedName.toUpperCase(), tModRec, tModRec.getSourceDepth(), tMod.getSourceDepth());
            upFlippedPhase = SeismicPhaseFactory.createPhase(reversedName.toLowerCase().replace("i", "y"), tModRec, tModRec.getSourceDepth(), tMod.getSourceDepth());
            endsDowngoingFlippedPhase = SeismicPhaseFactory.createPhase(reversedName.toUpperCase()+"ed", tModRec, tModRec.getSourceDepth(), tMod.getSourceDepth());
        } else {
            phase = SeismicPhaseFactory.createPhase(phaseName.toUpperCase(), tMod, tMod.getSourceDepth(), receiverDepth);
            //upPhase = SeismicPhaseFactory.createPhase(phaseName.toLowerCase(), tMod, tMod.getSourceDepth(), receiverDepth);
            upPhase = null;
            endsDowngoingPhase = SeismicPhaseFactory.createPhase(phaseName.toUpperCase()+"ed", tMod, tMod.getSourceDepth(), receiverDepth);
            flippedPhase = SeismicPhaseFactory.createPhase(reversedName.toUpperCase(), tModRec, tModRec.getSourceDepth(), tMod.getSourceDepth());
            upFlippedPhase = SeismicPhaseFactory.createPhase(reversedName.toLowerCase().replace("i", "y"), tModRec, tModRec.getSourceDepth(), tMod.getSourceDepth());
            endsDowngoingFlippedPhase = null;
        }
        if (receiverDepth != 0) {
            // where we end up, depending on if we end going down or up
            int upgoingRecBranch = tMod.findBranch(receiverDepth);
            int downgoingRecBranch = upgoingRecBranch - 1; // one branch shallower
            if (phase.getFinalPhaseSegment().isDownGoing ) {
                // downgoing at receiver
                assertTrue(phase.getFinalPhaseSegment().maxRayParam <= tMod.getTauBranch(downgoingRecBranch,
                                        phase.getFinalPhaseSegment().isPWave).getMinTurnRayParam(),
                        "max "+phase.getFinalPhaseSegment().maxRayParam
                                +" mod "+tMod.getTauBranch(downgoingRecBranch,
                                phase.getFinalPhaseSegment().isPWave).getMinTurnRayParam());
            } else {
                // upgoing at receiver
                assertTrue(phase.getFinalPhaseSegment().maxRayParam <= tMod.getTauBranch(upgoingRecBranch,
                                        phase.getFinalPhaseSegment().isPWave).getMaxRayParam());
            }

        }
        for (double degrees = 0; degrees < phase.getMaxDistance() && degrees < flippedPhase.getMaxDistance(); degrees+= distStep) {
            String pre = phaseName+" sd="+tMod.getSourceDepth()+" rd="+receiverDepth+" deg="+degrees;
            DistanceRay distanceRay = DistanceRay.ofDegrees(degrees);
            List<Arrival> phaseArrivals = distanceRay.calculate(phase);
            if (upPhase != null) {
                phaseArrivals.addAll(distanceRay.calculate(upPhase));
            }
            if (endsDowngoingPhase != null) {
                phaseArrivals.addAll(distanceRay.calculate(endsDowngoingPhase));
            }
            List<Arrival> flippedArrivals = distanceRay.calculate(flippedPhase);
            if (endsDowngoingFlippedPhase != null) {
                flippedArrivals.addAll(distanceRay.calculate(endsDowngoingFlippedPhase));
            }
            if (upFlippedPhase != null) {
                flippedArrivals.addAll(distanceRay.calculate(upFlippedPhase));
            }
            assertEquals(  phaseArrivals.size(), flippedArrivals.size(), pre+" arrival size "+phase.getName()+" "+flippedPhase.getName());
            for (int i = 0; i < phaseArrivals.size(); i++) {
                Arrival a = phaseArrivals.get(i);
                Arrival f = flippedArrivals.get(i);
                assertEquals(  a.getTime(), f.getTime(), 0.0001, a+" "+f);
                assertEquals(  a.getTakeoffAngleDegree(), f.getIncidentAngleDegree(), 0.0001);
                assertEquals(  a.getIncidentAngleDegree(), f.getTakeoffAngleDegree(), 0.0001);
                assertEquals(  a.getDist(), f.getDist(), 0.0001);
                assertEquals(  a.getRayParam(), f.getRayParam(), 0.0001);
            }
        }
    }
    

    @Test
    public void testOneDepthPcP() throws Exception {
        String modelName = "prem";
        TauModel tMod = TauModelLoader.load(modelName);
        testOneDepthPcPForModel(tMod);
    }
    
    @Test
    public void testOneDepthPcPConst() throws Exception {
        VelocityModel vMod = ConstantModelTest.createVelModLiquidOuterCore(1,  1);
        SphericalSModel smod = new SphericalSModel(vMod,
                                   0.1,
                                   11.0,
                                   115.0,
                                   2.5 * Math.PI / 180,
                                   0.01,
                                   true,
                                   SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        TauModel tMod = new TauModel(smod);
        testOneDepthPcPForModel(tMod);
    }
    
    public void testOneDepthPcPForModel(TauModel tMod) throws TauModelException {
        double depth = 500;
        TauModel tModDepth = tMod.depthCorrect(depth);
        double recDepth = 200;
        TauModel tModRecDepth = tModDepth.splitBranch(recDepth);
                
        TauModel flippedMod = tMod.depthCorrect(recDepth);
        flippedMod = flippedMod.splitBranch(depth);
                

        SeismicPhase PcP = SeismicPhaseFactory.createPhase("Pcp", tModRecDepth, tModRecDepth.getSourceDepth(),0);
        SeismicPhase p = SeismicPhaseFactory.createPhase("p", flippedMod, flippedMod.getSourceDepth(), 0);
        SeismicPhase PcP200 = SeismicPhaseFactory.createPhase("Pcp", tModRecDepth, tModRecDepth.getSourceDepth(), recDepth);
        double degrees = 0;
        List<Arrival> PcPArrivals = DistanceRay.ofDegrees(degrees).calculate(PcP);
        List<Arrival> pArrivals = DistanceRay.ofDegrees(degrees).calculate(p);
        List<Arrival> PcP200Arrivals = DistanceRay.ofDegrees(degrees).calculate(PcP200);
        String pre = "PcP "+recDepth;

        Arrival aPcP = PcPArrivals.get(0);
        Arrival ap = pArrivals.get(0);
        Arrival aPcP200 = PcP200Arrivals.get(0);
        assertEquals(  aPcP.getTime(), aPcP200.getTime()+ap.getTime(), 0.0001);
        assertEquals(  aPcP.getDist(), aPcP200.getDist()+ap.getDist(), 0.0001);
        assertEquals(  aPcP.getRayParam(), aPcP200.getRayParam(), 0.0001);
        assertEquals(  aPcP.getRayParam(), ap.getRayParam(), 0.0001);
    }
    
    @Test
    public void testCloseDepths()  throws Exception {
        float srcDepth = 2.39f;
        float recDepth = 2.4f;
        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName);
        TauModel tModDepth = tMod.depthCorrect(srcDepth);
        TauModel tModRecDepth = tModDepth.splitBranch(recDepth);
        TauModel flippedMod = tMod.depthCorrect(recDepth);
        flippedMod = flippedMod.splitBranch(srcDepth);
        System.out.println("Check P source="+srcDepth+" receiver="+recDepth);
        check(tModRecDepth, flippedMod, "P", .1);
        System.out.println("Check S source="+srcDepth+" receiver="+recDepth);
        check(tModRecDepth, flippedMod, "S", .1);
    }

    @Test
    public void testOuterCoreRec() throws Exception {
        float srcDepth = 2.39f;
        float recDepth = 3000f;
        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName);
        TauModel tModDepth = tMod.depthCorrect(srcDepth);
        TauModel tModRecDepth = tModDepth.splitBranch(recDepth);
        TauModel flippedMod = tMod.depthCorrect(recDepth);
        flippedMod = flippedMod.splitBranch(srcDepth);
        System.out.println("Check P source="+srcDepth+" receiver="+recDepth);
        check(tModRecDepth, flippedMod, "PK", .1);
        System.out.println("Check S source="+srcDepth+" receiver="+recDepth);
        check(tModRecDepth, flippedMod, "SK", .1);
    }

    @Test
    public void testInnerCoreRec() throws Exception {
        float srcDepth = 2.39f;
        float recDepth = 5500f;
        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName);
        TauModel tModDepth = tMod.depthCorrect(srcDepth);
        TauModel tModRecDepth = tModDepth.splitBranch(recDepth);
        TauModel flippedMod = tMod.depthCorrect(recDepth);
        flippedMod = flippedMod.splitBranch(srcDepth);
        System.out.println("Check P source="+srcDepth+" receiver="+recDepth);
        check(tModRecDepth, flippedMod, "PKI", .1);
        System.out.println("Check S source="+srcDepth+" receiver="+recDepth);
        check(tModRecDepth, flippedMod, "SKI", .1);
    }

    @Test
    public void testInnerCoreRecConvPhase() throws Exception {
        float srcDepth = 2.39f;
        float recDepth = 6000f;
        float surfaceRecDepth = 0;
        String modelName = "outerCoreDiscon.nd";
        VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
        TauModel tMod = TauModelLoader.createTauModel(vMod);
        TauModel tModDepth = tMod.depthCorrect(srcDepth);
        TauModel tModRecDepth = tModDepth.splitBranch(recDepth);
        assertTrue(SeismicPhaseFactory.createPhase("PKIed5500Jed", tModRecDepth, srcDepth, recDepth).hasArrivals());
        assertTrue(SeismicPhaseFactory.createPhase("PKIed5500I", tModRecDepth, srcDepth, recDepth).hasArrivals());
        assertTrue(SeismicPhaseFactory.createPhase("PKI5500jkp", tModRecDepth, srcDepth, surfaceRecDepth).hasArrivals());
    }
}
