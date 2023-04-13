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
}
