package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OutputsTest {

    @Test
    public void testPad() {
        assertEquals(9, Outputs.padCenter(" - ", 9).length());
        assertEquals(9, Outputs.padRight(" - ", 9).length());
        assertEquals(9, Outputs.padLeft(" - ", 9).length());
        assertTrue(Outputs.padRight("-", 3).equals("-  "));
        assertTrue(Outputs.padLeft("-", 3).equals("  -"));
        System.err.println();
        String padR = Outputs.padRight("-", (3+1)/2);
        assertEquals("- ", padR);
        assertEquals(" - ", Outputs.padLeft(padR, 3));
        assertEquals(" - ", Outputs.padCenter("-", 3));
        assertEquals("  (deg) ", Outputs.padCenter("(deg)", 8));
    }
}
