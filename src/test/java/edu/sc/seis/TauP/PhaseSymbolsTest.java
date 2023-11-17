package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import static edu.sc.seis.TauP.PhaseSymbols.isDiffracted;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
