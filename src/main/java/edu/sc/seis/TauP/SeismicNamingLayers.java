package edu.sc.seis.TauP;

import java.util.HashMap;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.TURN;

public class SeismicNamingLayers {

    public SeismicNamingLayers(TauModel tMod) {
        this.tMod = tMod;
        if (tMod.getIocbBranch() != tMod.getNumBranches()) {
            // inner core
            layersToLegNames.put(List.of(tMod.getIocbBranch(), tMod.getNumBranches()-1), List.of("I", "y", "J", "j"));
        }
        if (tMod.getCmbBranch() != tMod.getNumBranches() && tMod.getCmbBranch() != tMod.getIocbBranch()) {
            // liquid outer core
            layersToLegNames.put(List.of(tMod.getCmbBranch(), tMod.getIocbBranch()-1), List.of("K", "k", "", ""));
        }
        if (tMod.getCmbBranch() != 0) {
            // crust/mantle
            layersToLegNames.put(List.of(0, tMod.getCmbBranch()-1), List.of("P", "p", "S", "s"));
        }
    }

    /**
     * Gets the leg name character, like P, K or I or y for a branch, depending on
     * P vs S and up vs downgoing.
     * @param branchNum branch number
     * @param isPWave true if P wave
     * @param isDowngoing true if downgoing
     * @return leg char
     */
    public String legCharForBranch(int branchNum, boolean isPWave, boolean isDowngoing) {
        int nameIdx = isPWave ? ( isDowngoing ? 0 : 1) : ( isDowngoing ? 2 : 3);
        return layersToLegNames.get(rangeForBranchNum(branchNum)).get(nameIdx);
    }

    /**
     * Gets the leg name, like Ped, p, K or I or y for a branch, depending on
     * P vs S and up vs downgoing.
     * @param branchNum branch number
     * @param isPWave true if P wave
     * @param layerPropogationType up, down, head, etc
     * @param endAction end action for branch, ed not appended to TURN
     * @return leg name
     */
    public String legNameForTauBranch(int branchNum, boolean isPWave,
                                             LayerPropogationType layerPropogationType,
                                             PhaseInteraction endAction) {
        // replace with SeismicNamingLayer
        if (branchNum < 0 || branchNum >= tMod.getNumBranches()) {
            return "unknown";
        }
        boolean isDowngoing = true;
        if (layerPropogationType == LayerPropogationType.UP) {
            isDowngoing = false;
        }
        String legName = legCharForBranch(branchNum, isPWave, isDowngoing);
        if (layerPropogationType == LayerPropogationType.DOWN && endAction != TURN) {
            return legName+"ed";
        } else {
            // UP
            return legName;
        }
    }

    public int disconBranchAbove(int branchNum) {
        return rangeForBranchNum(branchNum).get(0);
    }

    public int disconBranchBelow(int branchNum) {
        if (branchNum == tMod.getNumBranches()) {
            // special case for center of earth as no discon at center
            return branchNum;
        }
        return rangeForBranchNum(branchNum).get(1)+1;
    }

    /**
     * Gets [top, bot] for range containing the given branchNum, with
     * top <= branchNum <= bot.
     * @param branchNum search for number
     * @return range that include brnachNum
     */
    public List<Integer> rangeForBranchNum(int branchNum) {
        for (List<Integer> range : layersToLegNames.keySet()) {
            if (range.get(0) <= branchNum && branchNum <= range.get(1)) {
                return range;
            }
        }
        throw new IllegalArgumentException("branch number "+branchNum+" beyond center of model: "+tMod.getNumBranches());
    }


    HashMap<List<Integer>, List<String>> layersToLegNames = new HashMap<>();

    TauModel tMod;
}
