package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PuristNameTest {

    @Test
    public void puristName() throws TauModelException {
        List<List<String>> names = new ArrayList<>();
        names.add(List.of("P660diff^410P", "P660diffp^410P"));
        names.add(List.of("P660diff^410P660diff", "P660diffp^410P660diff"));

        TauModel tauModel = TauModelLoader.load("ak135fcont");
        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel());
            assertEquals(ph.get(1), sp.getPuristName(), ph.get(0)+" -> "+ph.get(1));
        }
    }
}
