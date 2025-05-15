package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompositePhaseTest {

    TauModel tMod;

    @BeforeEach
    public void setUp() throws Exception {
        tMod = TauModelLoader.load("iasp91");
    }

    @Test
    public void testName() throws TauModelException {
        List<String> nameList = List.of("Scp", "Pcp", "PKp", "P", "S");
        for (String name : nameList) {
            SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod);
            if (phase instanceof CompositeSeismicPhase) {
                for (SimpleContigSeismicPhase scp : ((CompositeSeismicPhase) phase).getSubPhaseList()) {
                    assertEquals(name, scp.getName());
                    assertEquals(name, scp.getPuristName());
                }
            }
            assertEquals(name, phase.getName());
            assertEquals(name, phase.getPuristName());
        }
    }
    @Test
    public void testPKPName() throws TauModelException {
        String name = "PKP";
        SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(name, tMod);
        if (phase instanceof CompositeSeismicPhase) {
            for (SimpleContigSeismicPhase scp : ((CompositeSeismicPhase) phase).getSubPhaseList()) {
                assertEquals(name, scp.name);
            }
        }
        assertEquals(name,  phase.getName());
    }
}
