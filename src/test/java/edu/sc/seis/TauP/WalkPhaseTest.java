package edu.sc.seis.TauP;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalkPhaseTest {

    @Test
    public void shortWalk() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        int maxLegs = 1;
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<List<SeismicPhaseSegment>> walk = walker.findEndingPaths(maxLegs);
        for (List<SeismicPhaseSegment> segList : walk) {
            System.err.print(walker.phaseNameForSegments(segList));
            System.err.println();
        }
        System.err.println("Found "+walk.size()+" segments < "+maxLegs);
    }

    @Test
    public void mergeTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, false, "p", 0, 100));
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
        System.err.println(upper);
        System.err.println(lower);

        assertTrue(walker.canMergePhases(upper, lower));
        List<SeismicPhaseSegment> merged = walker.mergePhases(upper, lower);
        assertEquals(merged.get(merged.size()-1).maxRayParam, 100);
        assertEquals(merged.get(merged.size()-1).minRayParam, 0);

    }
    @Test
    public void mergeUndersideTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, false, "p", 0, 100));
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        lower.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
        System.err.println(upper);
        System.err.println(lower);

        assertTrue(walker.canMergePhases(upper, lower));
        List<SeismicPhaseSegment> merged = walker.mergePhases(upper, lower);
        assertEquals(merged.get(merged.size()-1).maxRayParam, 100);
        assertEquals(merged.get(merged.size()-1).minRayParam, 0);

    }
    @Test
    public void mergeUnderside_pP() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> upper = new ArrayList<>();
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, TURN, true, "P", 0, 100));
        upper.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, END, false, "p", 0, 100));
        List<SeismicPhaseSegment> lower = new ArrayList<>();
        lower.add(new SeismicPhaseSegment(tMod, 0, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 100));
        lower.add(new SeismicPhaseSegment(tMod, 0, 2, isPWave, TURN, true, "P", 0, 100));
        lower.add(new SeismicPhaseSegment(tMod, 2, 0, isPWave, END, false, "p", 0, 100));
        System.err.println(upper);
        System.err.println(lower);

        assertTrue(walker.canMergePhases(upper, lower));
        List<SeismicPhaseSegment> merged = walker.mergePhases(upper, lower);
        assertEquals(merged.get(merged.size()-1).maxRayParam, 100);
        assertEquals(merged.get(merged.size()-1).minRayParam, 0);

    }

    @Test
    public void interactNumTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> turnOnly = new ArrayList<>();
        turnOnly.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        turnOnly.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
        int num = walker.calcInteractionNumber(turnOnly);
        assertEquals(0, num);

        List<SeismicPhaseSegment> reflUnder = new ArrayList<>();
        reflUnder.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, REFLECT_UNDERSIDE, false, "p", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TURN, true, "P", 0, 10));
        reflUnder.add(new SeismicPhaseSegment(tMod, 1, 0, isPWave, END, false, "p", 0, 10));
        assertEquals(1, walker.calcInteractionNumber(reflUnder));
    }
    @Test
    public void interactConvTest() throws TauModelException {
        boolean isPWave = true;
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk(tMod);
        List<SeismicPhaseSegment> convTransDown = new ArrayList<>();
        convTransDown.add(new SeismicPhaseSegment(tMod, 0, 1, isPWave, TRANSDOWN, true, "P", 0, 10));
        convTransDown.add(new SeismicPhaseSegment(tMod, 1, 2, !isPWave, TURN, false, "s", 0, 10));
        convTransDown.add(new SeismicPhaseSegment(tMod, 2, 0, !isPWave, END, false, "s", 0, 10));
        assertEquals(1,  walker.calcInteractionNumber(convTransDown));


    }
}
