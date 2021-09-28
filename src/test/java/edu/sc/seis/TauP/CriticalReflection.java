package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CriticalReflection {


    @Test
    public void testTopCricitalReflections() throws Exception {
        compareReflections("PKviKP", "PKViKP", 120.0);
        compareReflections("Svcs", "SVcs", 99.2);
        compareReflections("Sv660s", "SV660s", 25.0);
        compareReflections("Sv410s", "SV410s", 15.0);
        compareReflections("Svms", "SVms", 1.0);
    }
    @Test
    @Disabled
    public void testUnderCricitalReflections() throws Exception {
        // doesn't work as phase has to be able to get to surface,
        // maybe try with receiver at depth?
        compareReflections("P^mP", "P^xmP", 99.2, 100.0);
    }
    public void compareReflections(String reflPhase, String critReflPhase, double degrees) throws Exception {
        compareReflections( reflPhase,  critReflPhase,  degrees, 0.0);
    }

    public void compareReflections(String reflPhase, String critReflPhase, double degrees, double depth) throws Exception {
        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName);
        SeismicPhase PcP = new SeismicPhase(reflPhase, tMod);
        SeismicPhase PcP_crit = new SeismicPhase(critReflPhase, tMod);

        List<Arrival> ScSArrivals = PcP.calcTime(degrees);
        List<Arrival> ScS_crit_Arrivals = PcP_crit.calcTime(degrees);

        Arrival aScS = ScSArrivals.get(0);
        Arrival aScS_crit = ScS_crit_Arrivals.get(0);
        assertEquals(  aScS.getTime(), aScS_crit.getTime(), 0.0001);
        assertEquals(  aScS.getDist(), aScS_crit.getDist(), 0.0001);
        assertEquals(  aScS.getRayParam(), aScS_crit.getRayParam(), 0.0001);
    }
 }
