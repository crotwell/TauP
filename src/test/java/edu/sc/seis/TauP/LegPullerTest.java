package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class LegPullerTest {

    @Test
    public void testLegPull_P410diff() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("P410diff");
        assertEquals(2, legs.size());
        assertEquals("P410diff", legs.get(0));
        assertEquals("END", legs.get(1));
    }

    @Test
    public void testLegPull_P410n() throws Exception {
        ArrayList<String> legs = LegPuller.legPuller("P410n");
        assertEquals(2, legs.size());
        assertEquals("P410n", legs.get(0));
        assertEquals("END", legs.get(1));
    }
}
