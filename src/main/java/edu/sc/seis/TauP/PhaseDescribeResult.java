package edu.sc.seis.TauP;

import java.util.List;

public class PhaseDescribeResult extends AbstractPhaseResult {

    public PhaseDescribeResult(String modelName, List<Double> depth, List<Double> receiverDepth,
                               List<PhaseName> phaseNameList, Scatterer scatterer, List<PhaseDescription> phaseDescList
                      ) {
        super(modelName, depth, receiverDepth, phaseNameList, scatterer);
        this.phaseDescList = phaseDescList;
    }

    public List<PhaseDescription> getPhaseDescList() {
        return phaseDescList;
    }

    List<PhaseDescription> phaseDescList;
}
