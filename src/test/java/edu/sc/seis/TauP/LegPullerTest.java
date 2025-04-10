package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseSymbols.DIFF;
import static org.junit.jupiter.api.Assertions.*;

public class LegPullerTest {

    @Test
    public void testLegalPhases() throws Exception {
        for (String name : IllegalPhasesTest.otherLegalPhases) {
            ArrayList<String> legs = LegPuller.legPuller(name);
            assertNotEquals(0, legs.size(), name);
        }
    }

    @Test
    public void testDiffSubstring() throws Exception {
        String name = "PKdiffP";
        int offset = 1;
        String diffLeg = name.substring(offset, name.indexOf(DIFF, offset + 1) + DIFF.length());
        assertEquals("Kdiff", diffLeg);
    }

    @Test
    public void testLegPull_P410diff() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("P410diff");
        assertEquals(2, legs.size());
        assertEquals("P410diff", legs.get(0));
        assertEquals("END", legs.get(1));
    }

    @Test
    public void testLegPull_P20n() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("P20n");
        assertEquals(2, legs.size());
        assertEquals("P20n", legs.get(0));
        assertEquals("END", legs.get(1));
    }
    @Test
    public void testLegPull_P410n() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("P410n");
        assertEquals(2, legs.size());
        assertEquals("P410n", legs.get(0));
        assertEquals("END", legs.get(1));
    }

    @Test
    public void testLegPull_PKdiffP() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("PKdiffP");
        assertEquals(4, legs.size());
        assertEquals("P", legs.get(0));
        assertEquals("Kdiff", legs.get(1));
        assertEquals("P", legs.get(2));
        assertEquals("END", legs.get(3));
    }

    @Test
    public void testLegPull_PK3000diffP() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("PK3000diffP");
        assertEquals(4, legs.size());
        assertEquals("P", legs.get(0));
        assertEquals("K3000diff", legs.get(1));
        assertEquals("P", legs.get(2));
        assertEquals("END", legs.get(3));
    }

    @Test
    public void testLegPull_PK3000nP() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("PK3000nP");
        assertEquals(4, legs.size());
        assertEquals("P", legs.get(0));
        assertEquals("K3000n", legs.get(1));
        assertEquals("P", legs.get(2));
        assertEquals("END", legs.get(3));
    }
    @Test
    public void testLegPull_PKI5500diffP() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("PKI5500diffKP");
        assertEquals(6, legs.size());
        assertEquals("P", legs.get(0));
        assertEquals("K", legs.get(1));
        assertEquals("I5500diff", legs.get(2));
        assertEquals("K", legs.get(3));
        assertEquals("P", legs.get(4));
        assertEquals("END", legs.get(5));
    }

    @Test
    public void pullCustomNamedDiscon() throws Exception {
        String customDiscon = PhaseSymbols.NAMED_DISCON_START+"my-discon"+PhaseSymbols.NAMED_DISCON_END;
        String phaseName = "P"+customDiscon+"diff";
        String boundId = LegPuller.extractBoundaryId(phaseName, 1, false);
        assertEquals(customDiscon, boundId);
        ArrayList<String> legs = LegPuller.legPuller(phaseName);
        assertEquals(2, legs.size());
        assertEquals(phaseName, legs.get(0));
        assertEquals("END", legs.get(1));

    }

    @Test
    public void pullCustomNamedDisconB() throws Exception {
        String depthDisconPhase = "S410PcP";
        ArrayList<String> simplegs = LegPuller.legPuller(depthDisconPhase);
        assertEquals(6, simplegs.size(), simplegs.toString());

        String customDiscon = PhaseSymbols.NAMED_DISCON_START+"liquid-silicate"+PhaseSymbols.NAMED_DISCON_END;
        String phaseName = "S"+customDiscon+"PKP"+customDiscon+"s";
        String boundId = LegPuller.extractBoundaryId(phaseName, 1, false);
        assertEquals(customDiscon, boundId);
        ArrayList<String> legs = LegPuller.legPuller(phaseName);
        assertEquals(8, legs.size());
        assertEquals("S", legs.get(0));
        assertEquals(customDiscon, legs.get(1));
        assertEquals("P", legs.get(2));
        assertEquals("K", legs.get(3));
        assertEquals("P", legs.get(4));
        assertEquals(customDiscon, legs.get(5));
        assertEquals("s", legs.get(6));
        assertEquals("END", legs.get(7));

        customDiscon = "S_liquid-silicate_PKP_liquid-silicate_S";
    }

    @Test
    public void extractBoundaryId() throws PhaseParseException {
        String phaseName = "K3000diff";
        String boundId = LegPuller.extractBoundaryId(phaseName, 1, false);
        assertEquals("3000", boundId);
    }
}
