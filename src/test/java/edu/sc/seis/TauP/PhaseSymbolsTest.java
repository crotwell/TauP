package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import static edu.sc.seis.TauP.PhaseSymbols.*;
import static org.junit.jupiter.api.Assertions.*;

public class PhaseSymbolsTest {


    @Test
    public void testDiff() throws Exception {
        System.err.println("PhaseSymboTest "+LegPuller.headDiffRE);
        assertTrue(isDiffracted("Pdiff"));
        assertTrue(isDiffracted("PdiffS"));
        assertTrue(isDiffracted("P410diff"));
        assertTrue(isDiffracted("Kdiff"));
        assertTrue(isDiffracted("PKdiff", 1));
        assertTrue(isDiffracted("PKdiffP", 1));
        assertTrue(isDiffracted("Pmdiff"));
    }



    @Test
    public void testHead() throws Exception {
        System.err.println("PhaseSymboTest " + LegPuller.headDiffRE);
        assertFalse(isHead("PS", 0));
        assertTrue(isHead("Pn", 0));
        assertTrue(isHead("P410n", 0));
        assertTrue(isHead("PK3010nkp", 1));
        assertFalse(isHead("P410p", 0));
    }


    @Test
    public void testDiffDown() throws Exception {
        System.err.println("PhaseSymboTest "+LegPuller.headDiffRE);
        assertFalse(isDiffractedDown("Pdiff"));
        assertTrue(isDiffractedDown("Pdiffdn"));
        assertFalse(isDiffractedDown("P410diff"));
        assertTrue(isDiffractedDown("P410diffdn"));
        assertTrue(isDiffractedDown("Kdiffdn"));
        assertTrue(isDiffractedDown("PKdiffdn", 1));
        assertTrue(isDiffractedDown("PKdiffdnIKP", 1));
        assertTrue(isDiffractedDown("Pmdiffdn"));
        assertTrue(isDiffractedDown("P1607diffdnPedcP1607.753diff"));
    }

    @Test
    public void testPhaseSacHeaderSingle() throws Exception {

        PhaseName pn_single = PhaseName.parseName("P-2");
        assertEquals("P", pn_single.getName());
        assertEquals(1, pn_single.sacTNumTriplication.size());
        assertEquals(2, pn_single.sacTNum);
    }

    @Test
    public void testPhaseSacHeaderMultiple() throws Exception {
        PhaseName pn = PhaseName.parseName("P-234");
        assertEquals("P", pn.getName());
        assertEquals(3, pn.sacTNumTriplication.size());
    }

    @Test
    public void testPhaseSacHeaderEmpty() throws Exception {
        PhaseName purename = PhaseName.parseName("P");
        assertEquals("P", purename.getName());
        assertEquals(0, purename.sacTNumTriplication.size());
    }
}
