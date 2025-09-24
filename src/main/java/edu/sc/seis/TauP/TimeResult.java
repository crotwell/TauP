package edu.sc.seis.TauP;

import java.util.List;

public class TimeResult extends AbstractPhaseResult {

    public TimeResult(String modelName, List<Double> depth, List<Double> receiverDepth,
                      List<PhaseName> phaseNameList, Scatterer scatterer,
                      boolean withAmp, SeismicSource sourceArgs, List<Arrival> arrivals) {
        super(modelName, depth, receiverDepth, phaseNameList, scatterer, withAmp, sourceArgs);
        this.arrivals = arrivals;
    }

    public List<Arrival> getArrivals() {
        return arrivals;
    }

    List<Arrival> arrivals;
}
