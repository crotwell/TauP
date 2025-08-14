package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WavefrontResult extends AbstractPhaseResult {

    public WavefrontResult(String modelName, List<Double> depth, List<Double> receiverDepth,
                           List<PhaseName> phaseNameList, Scatterer scatterer,
                           Map<Double, List<PhaseIsochron>> isochronMap) {
        super(modelName, depth, receiverDepth, phaseNameList, scatterer);
        timesteps = new ArrayList<>(isochronMap.keySet());
        Collections.sort(timesteps);
        for (Double d : timesteps) {
            isochrons.add(new Isochron(d, isochronMap.get(d)));
        }
    }

    public List<Double> getTimesteps() {
        return timesteps;
    }

    public List<Isochron> getIsochrons() {
        return isochrons;
    }

    List<Double> timesteps;
    List<Isochron> isochrons = new ArrayList<>();
}
