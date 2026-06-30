package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TauP_FindTest {


    @Test
    public void Pn_namedDisconWalk() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        int maxActions = 1;
        TauP_Find find = new TauP_Find();
        // only  moho, cmb, iocb
        find.onlyNamedDiscon = true;
        find.onlyPWave = true;
        find.excludeDepthNames.addAll(List.of("20", "210", "410", "660", "2889", "5153.9")); // moho 35, iocb 5153.9
        double receiverDepth = 0;
        SeismicPhaseWalk allwalker = find.createWalker(tMod, receiverDepth, find.getExcludedDepths(tMod));
        List<ProtoSeismicPhase> allwalk = allwalker.findEndingPaths(maxActions);


        for (int i = 0; i < allwalk.size()-1; i++) {
            ProtoSeismicPhase proto = allwalk.get(i);
            assertEquals(receiverDepth, proto.endSegment().getEndDepth(), proto.getPuristName());
            for (ProtoSeismicPhase np : allwalk.subList(i+1, allwalk.size())) {
                assertNotEquals(np.getPuristName(), proto.getPuristName(), "Duplicate phase: ");
            }
        }

        List<String> zeroActionNames = List.of("P", "PKp", "PKIkp");
        List<String> oneActionPBounceNames = List.of("Pvmp", "P^mP", "PP",
                "PKpPKp","PKp^mPKp",
                "PKIkpPKIkp", "PKIkp^mPKIkp");
        List<String> diffHeadNames = List.of("Pn", "P35diff" );
        List<String> oneAction = new ArrayList<>();
        oneAction.addAll(zeroActionNames);
        oneAction.addAll(oneActionPBounceNames);
        oneAction.addAll(diffHeadNames);

        for (String phase : oneAction) {
            boolean notFound = true;
            for (ProtoSeismicPhase proto : allwalk) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "allWalk does not contain one action phase: "+phase);
        }
        for (ProtoSeismicPhase proto : allwalk) {
            boolean notFound = true;
            for (String phase : oneAction) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "Found extra, List does not contain phase: "+proto.getPuristName()+"\n"+proto.branchNumSeqStrWithSegBreaks()+"\n"+proto.asSeismicPhase().describe());
        }
        for (int i = 0; i < allwalk.size()-1; i++) {
            for (ProtoSeismicPhase np : allwalk.subList(i+1, allwalk.size())) {
                assertNotEquals(np.getPuristName(), allwalk.get(i).getPuristName(), "Duplicate phase: ");
            }

        }

        assertEquals(oneAction.size(), allwalk.size(), "size of one action walk");
    }

    @Test
    public void namedDisconWalk() throws TauModelException {
        TauModel tMod = TauModelLoader.load("iasp91");
        int maxActions = 1;
        TauP_Find find = new TauP_Find();
        // only  moho, cmb, iocb
        find.onlyNamedDiscon = true;
        find.excludeDepthNames.addAll(List.of("20", "210", "410", "660")); // moho 35, iocb 5153.9
        double receiverDepth = 0;
        SeismicPhaseWalk allwalker = find.createWalker(tMod, receiverDepth, find.getExcludedDepths(tMod));
        List<ProtoSeismicPhase> allwalk = allwalker.findEndingPaths(maxActions);

        List<String> zeroActionNames = List.of("P", "S", "PKp", "PKIkp");
        List<String> oneActionPBounceNames = List.of("Pvmp", "P^mP", "PP", "Pcp", "PKKp", "PKikp",
                "PKpPKp","PKp^mPKp",
                "PKIIkp", "PKIkKIkp", "PKIkpPKIkp", "PKIkp^mPKIkp");
        List<String> oneActionSBounceNames = List.of("Svms", "S^mS", "SS", "Scs");
        List<String> oneActionConvertNames = List.of("PedmS","Pms", "SedmP", "SedmPKp", "SedmPKIkp",
                "PKpms", "PKIkpms",
                "Smp", "SKp", "SKIkp", "PKs", "PKIks"); // PKJkp has 2
        List<String> diffHeadNames = List.of("Pn", "Sn", "P35diff", "S35diff", "Pdiff", "Sdiff", "PKdiffp", "PKI5154nkp" );
        List<String> oneAction = new ArrayList<>();
        oneAction.addAll(zeroActionNames);
        oneAction.addAll(oneActionPBounceNames);
        oneAction.addAll(oneActionSBounceNames);
        oneAction.addAll(oneActionConvertNames);
        oneAction.addAll(diffHeadNames);

        for (String phase : oneAction) {
            boolean notFound = true;
            for (ProtoSeismicPhase proto : allwalk) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "allwalk does not contain one action phase: "+phase);
        }
        for (ProtoSeismicPhase proto : allwalk) {
            boolean notFound = true;
            for (String phase : oneAction) {
                if (proto.getPuristName().equals(phase)) {
                    notFound = false;
                }
            }
            assertFalse(notFound, "Found extra, List does not contain phase: "+proto.getPuristName());
        }
        for (int i = 0; i < allwalk.size()-1; i++) {
            for (ProtoSeismicPhase np : allwalk.subList(i+1, allwalk.size())) {
                assertNotEquals(np.getPuristName(), allwalk.get(i).getPuristName(), "Duplicate phase: ");
            }

        }

        assertEquals(oneAction.size(), allwalk.size(), "size of one action walk");
    }

}
