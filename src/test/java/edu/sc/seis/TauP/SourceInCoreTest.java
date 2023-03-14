package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class SourceInCoreTest {

    List<String> outerCoreSourcePhases = Arrays.asList(new String[] {
            "KP", "kp", "KKs", "KiKP", "kKIKS", "k^cKP",
            "KI^iIKP", "Kedikp", "KedIkS", "KedIks"
    });
    List<String> innerCoreSourcePhases = Arrays.asList(new String[] {
            "IKP", "IkKIKs", "IKKiKP", "IIIKS"
    });

    @Test
    public void sourceInCore() throws TauModelException {
        boolean DEBUG = true;
        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName);
        float[] sourceDepths = {0, 1000, 2000, 3500, 6000 };
        float receiverDepth = 0;
        List<String> mantlePhases = TauP_Time.extractPhaseNames("ttall");
        List<String> legalPhases = new ArrayList<String>();
        legalPhases.addAll(mantlePhases);
        legalPhases.addAll(outerCoreSourcePhases);
        legalPhases.addAll(innerCoreSourcePhases);
        for (float sourceDepth : sourceDepths) {
            TauModel tModDepth = tMod.depthCorrect(sourceDepth);
            for (String phaseName : legalPhases) {
                try {
                    SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tModDepth, tModDepth.getSourceDepth(), receiverDepth, DEBUG);
                    if (sourceDepth > tMod.getCmbDepth() && mantlePhases.contains(phaseName)) {
                        assertEquals(-1, phase.getMaxRayParam());
                    }
                    if ((sourceDepth > tMod.getCmbDepth()  && sourceDepth < tMod.getIocbDepth() )
                            && (mantlePhases.contains(phaseName) || innerCoreSourcePhases.contains(phaseName))) {
                        assertEquals(-1, phase.getMaxRayParam());
                    }
                    if (sourceDepth > tMod.getIocbDepth() && (mantlePhases.contains(phaseName) || outerCoreSourcePhases.contains(phaseName))) {
                        assertEquals(-1, phase.getMaxRayParam());
                    }
                } catch (TauModelException ex) {
                    System.err.println("Working on phase: " + phaseName);
                    throw ex;
                }
            }
        }
    }

}
