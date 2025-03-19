package edu.sc.seis.TauP;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CriticalReflection {


    @Test
    public void testTopCricitalReflections() throws Exception {
        compareReflections("PKviKP", "PKViKP", 120.0);
        compareReflections("Svcs", "SVcs", 99.2);
        compareReflections("Sv660s", "SV660s", 25.0);
        compareReflections("Sv410s", "SV410s", 15.0);
        compareReflections("Svms", "SVms", 1.0);
        compareReflections("SPvmp", "SPVmp", 45.0);
    }
    @Test
    @Disabled
    public void testUnderCricitalReflections() throws Exception {
        // doesn't work as phase has to be able to get to surface,
        // and requires low velocity zone to have ray parameters that
        // generate critical reflection
        // maybe try with receiver at depth under LVZ?
        compareReflections("P^mS", "P^xmS", 90, 100.0);
    }
    public void compareReflections(String reflPhase, String critReflPhase, double degrees) throws Exception {
        compareReflections( reflPhase,  critReflPhase,  degrees, 0.0);
    }

    public void compareReflections(String reflPhaseName, String critReflPhaseName, double degrees, double depth)
            throws Exception {
        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
        SimpleSeismicPhase reflPhaseCalc = SeismicPhaseFactory.createPhase(reflPhaseName, tMod);
        assertInstanceOf(SimpleContigSeismicPhase.class, reflPhaseCalc);
        SimpleContigSeismicPhase reflPhase = (SimpleContigSeismicPhase)reflPhaseCalc;
        SimpleSeismicPhase critPhaseCalc = SeismicPhaseFactory.createPhase(critReflPhaseName, tMod);
        assertInstanceOf(SimpleContigSeismicPhase.class, critPhaseCalc);
        SimpleContigSeismicPhase critPhase = (SimpleContigSeismicPhase)critPhaseCalc;


        // critcial is subset of uncrit reflection
        assertTrue(reflPhase.getMinDistanceDeg() <= critPhase.getMinDistanceDeg());
        assertTrue(critPhase.getMaxDistanceDeg() <= reflPhase.getMaxDistanceDeg());
        assertTrue(reflPhase.getMinRayParam() <= critPhase.getMinRayParam());
        assertTrue(critPhase.getMaxRayParam() <= reflPhase.getMaxRayParam());
        assertEquals(reflPhase.getPhaseSegments().size(), critPhase.getPhaseSegments().size());
        for (int i = 0; i < reflPhase.getPhaseSegments().size(); i++) {
            SeismicPhaseSegment reflSeg = reflPhase.getPhaseSegments().get(i);
            SeismicPhaseSegment critSeg = critPhase.getPhaseSegments().get(i);
            assertEquals(reflSeg.startBranch, critSeg.startBranch);
            assertEquals(reflSeg.endBranch, critSeg.endBranch);
            assertEquals(reflSeg.isDownGoing, critSeg.isDownGoing);
            assertEquals(reflSeg.isPWave, critSeg.isPWave);
        }

        DistanceRay distanceRay = DistanceRay.ofDegrees(degrees);
        List<Arrival> reflArrivals = distanceRay.calculate(reflPhase);
        List<Arrival> critArrivals = distanceRay.calculate(critPhase);

        Arrival reflArrive = reflArrivals.get(0);
        Arrival critArrive = critArrivals.get(0);
        assertEquals(  reflArrive.getTime(), critArrive.getTime(), 0.0001);
        assertEquals(  reflArrive.getDist(), critArrive.getDist(), 0.0001);
        assertEquals(  reflArrive.getRayParam(), critArrive.getRayParam(), 0.0001);
    }
 }
