package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TauP_FindTest {

    @Test
    public void namedDisconWalk() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        int maxActions = 1;
        TauP_Find find = new TauP_Find();
        // only  moho, cmb, iocb
        find.onlyNamedDiscon = true;
        find.excludeDepthNames.addAll(List.of("20", "210", "410", "660"));
        double receiverDepth = 0;
        SeismicPhaseWalk allwalker = find.createWalker(tMod, receiverDepth, find.getExcludedDepths(tMod));
        List<ProtoSeismicPhase> allwalk = allwalker.findEndingPaths(maxActions);

        List<String> zeroActionNames = List.of("P", "S", "PKp", "PKIkp");
        List<String> oneActionPBounceNames = List.of("Pvmp", "P^mP", "PP", "Pcp", "PKKp", "PKikp",
                "PKpPKp","PKp^mPKp",
                "PKIIkp", "PKIkpPKIkp", "PKIkp^mPKIkp");
        List<String> oneActionSBounceNames = List.of("Svms", "S^mS", "SS", "Scs");
        List<String> oneActionConvertNames = List.of("PedmS","Pms", "SedmP", "SedmPKp", "SedmPKIkp",
                "PKpms", "PKIkpms",
                "Smp", "SKp", "SKIkp", "PKs", "PKIks"); // PKJkp has 2
        List<String> oneAction = new ArrayList<>();
        oneAction.addAll(zeroActionNames);
        oneAction.addAll(oneActionPBounceNames);
        oneAction.addAll(oneActionSBounceNames);
        oneAction.addAll(oneActionConvertNames);

        for (String phase : oneAction) {
            boolean notFound = true;
            for (ProtoSeismicPhase proto : allwalk) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "One action does not contain phase "+phase);
        }
        for (ProtoSeismicPhase proto : allwalk) {
            boolean notFound = true;
            for (String phase : oneAction) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "List does not contain phase "+proto.getPuristName());
        }

        assertEquals(oneAction.size(), allwalk.size(), "size of one action walk");
    }

}
