package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.List;

public class WalkPhaseTest {

    @Test
    public void shortWalk() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk();
        int maxLegs = 3;
        List<List<SeismicPhaseSegment>> walk = walker.walkPhases(tMod, 3);
        for (List<SeismicPhaseSegment> segList : walk) {
            System.err.print(walker.phaseNameForSegments(segList));
            System.err.println();
        }
        System.err.println("Found "+walk.size()+" segments < "+maxLegs);
    }
}
