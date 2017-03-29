package edu.sc.seis.TauP;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;


public class ReceiverAtDepth {

    @Test
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
        double receiverDepth = tModRec.getSourceDepth();
        SeismicPhase phase;
        SeismicPhase upPhase;
        SeismicPhase endsDowngoingPhase = null;
        SeismicPhase flippedPhase;
        SeismicPhase upFlippedPhase;
        SeismicPhase endsDowngoingFlippedPhase = null;
        if (tMod.getSourceDepth() == receiverDepth) {
            phase = new SeismicPhase(phaseName, tMod, receiverDepth);
            upPhase = null;
            endsDowngoingPhase = null;
            flippedPhase = new SeismicPhase(phaseName, tModRec, tMod.getSourceDepth());
            upFlippedPhase = null;
            endsDowngoingFlippedPhase = null;
        } else if (tMod.getSourceDepth() > receiverDepth) {
            phase = new SeismicPhase(phaseName.toUpperCase(), tMod, receiverDepth);
            upPhase = new SeismicPhase(phaseName.toLowerCase(), tMod, receiverDepth);
            endsDowngoingPhase = null;
            flippedPhase = new SeismicPhase(phaseName.toUpperCase(), tModRec, tMod.getSourceDepth());
            upFlippedPhase = new SeismicPhase(phaseName.toLowerCase(), tModRec, tMod.getSourceDepth());
            endsDowngoingFlippedPhase = new SeismicPhase(phaseName.toUpperCase()+"ed", tModRec, tMod.getSourceDepth());
        } else {
            phase = new SeismicPhase(phaseName.toUpperCase(), tMod, receiverDepth);
            upPhase = new SeismicPhase(phaseName.toLowerCase(), tMod, receiverDepth);
            endsDowngoingPhase = new SeismicPhase(phaseName.toUpperCase()+"ed", tMod, receiverDepth);
            flippedPhase = new SeismicPhase(phaseName.toUpperCase(), tModRec, tMod.getSourceDepth());
            upFlippedPhase = new SeismicPhase(phaseName.toLowerCase(), tModRec, tMod.getSourceDepth());
            endsDowngoingFlippedPhase = null;
        }
        double distStep = 11;
        for (double degrees = 0; degrees < phase.getMaxDistance() && degrees < flippedPhase.getMaxDistance(); degrees+= distStep) {
            String pre = phaseName+" sd="+tMod.getSourceDepth()+" rd="+receiverDepth+" deg="+degrees;
            List<Arrival> phaseArrivals = phase.calcTime(degrees);
            if (upPhase != null) {
                phaseArrivals.addAll(upPhase.calcTime(degrees));
            }
            if (endsDowngoingPhase != null) {
                phaseArrivals.addAll(endsDowngoingPhase.calcTime(degrees));
            }
            List<Arrival> flippedArrivals = flippedPhase.calcTime(degrees);
            if (endsDowngoingFlippedPhase != null) {
                flippedArrivals.addAll(endsDowngoingFlippedPhase.calcTime(degrees));
            }
            if (upFlippedPhase != null) {
                flippedArrivals.addAll(upFlippedPhase.calcTime(degrees));
            }
            assertEquals(pre+" arrival size "+phase.getName()+" "+flippedPhase.getName(),  phaseArrivals.size(), flippedArrivals.size());
            for (int i = 0; i < phaseArrivals.size(); i++) {
                Arrival a = phaseArrivals.get(i);
                Arrival f = flippedArrivals.get(i);
                assertEquals(pre+" time",  a.getTime(), f.getTime(), 0.0001);
                assertEquals(pre+" takeoff/incident",  a.getTakeoffAngle(), f.getIncidentAngle(), 0.0001);
                assertEquals(pre+" incident/takeoff",  a.getIncidentAngle(), f.getTakeoffAngle(), 0.0001);
                assertEquals(pre+" dist",  a.getDist(), f.getDist(), 0.0001);
                assertEquals(pre+" rayParam",  a.getRayParam(), f.getRayParam(), 0.0001);
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
                

        SeismicPhase PcP = new SeismicPhase("Pcp", tModRecDepth, 0);
        SeismicPhase p = new SeismicPhase("p", flippedMod, 0);
        SeismicPhase PcP200 = new SeismicPhase("Pcp", tModRecDepth, recDepth);
        double degrees = 0;
        List<Arrival> PcPArrivals = PcP.calcTime(degrees);
        List<Arrival> pArrivals = p.calcTime(degrees);
        List<Arrival> PcP200Arrivals = PcP200.calcTime(degrees);
        String pre = "PcP "+recDepth;

        Arrival aPcP = PcPArrivals.get(0);
        Arrival ap = pArrivals.get(0);
        Arrival aPcP200 = PcP200Arrivals.get(0);
        assertEquals(pre+" time",  aPcP.getTime(), aPcP200.getTime()+ap.getTime(), 0.0001);
        assertEquals(pre+" dist",  aPcP.getDist(), aPcP200.getDist()+ap.getDist(), 0.0001);
        assertEquals(pre+" rayParam PcP PcP200",  aPcP.getRayParam(), aPcP200.getRayParam(), 0.0001);
        assertEquals(pre+" rayParam PcP p",  aPcP.getRayParam(), ap.getRayParam(), 0.0001);
    }
    
}
