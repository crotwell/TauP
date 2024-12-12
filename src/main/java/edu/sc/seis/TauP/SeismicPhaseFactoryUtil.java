package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.PhaseInteraction.REFLECT_TOPSIDE;
import static edu.sc.seis.TauP.PhaseInteraction.REFLECT_TOPSIDE_CRITICAL;
import static edu.sc.seis.TauP.PhaseSymbols.*;
import static edu.sc.seis.TauP.SimpleSeismicPhase.PWAVE;
import static edu.sc.seis.TauP.SimpleSeismicPhase.SWAVE;

public class SeismicPhaseFactoryUtil {

    public static boolean isPWave(String currLeg, boolean prevIsP) {
        if(isCompressionalWaveSymbol(currLeg)) {
            return PWAVE;
        } else if(isTransverseWaveSymbol(currLeg)) {
            return SWAVE;
        }
        return prevIsP;
    }

    public static void doTopsideReflect(SeismicPhaseLayerFactory factory, ProtoSeismicPhase proto, String currLeg, int currBranch, String nextLeg) throws TauModelException {
        PhaseInteraction endAction;
        if(PhaseSymbols.isReflectSymbol(nextLeg)) {
            if (isCriticalReflectSymbol(nextLeg)) {
                endAction = REFLECT_TOPSIDE_CRITICAL;
            } else {
                endAction = REFLECT_TOPSIDE;
            }
            int disconBranch = LegPuller.closestDisconBranchToDepth(proto.tMod,
                    nextLeg.substring(1));
            if (currBranch <= disconBranch - 1) {
                boolean isPWave = proto.endSegment().isPWave;
                boolean nextIsPWave = isPWave(nextLeg, isPWave);
                proto.addToBranch(
                        disconBranch - 1,
                        isPWave,
                        nextIsPWave,
                        endAction,
                        currLeg);
            } else {
                proto.failNext("TopsideReflect Phase not recognized in " + factory.layerName + ": "
                        + currLeg + " followed by " + nextLeg
                        + " when currBranch=" + currBranch
                        + " < disconBranch=" + disconBranch);
            }
        }
    }

}
