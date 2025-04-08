package edu.sc.seis.TauP.gson;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.PhaseName;
import edu.sc.seis.TauP.Scatterer;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;

import java.util.List;

public class TimeResult extends AbstractPhaseResult {

    public TimeResult(String modelName, List<Double> depth, List<Double> receiverDepth,
                      List<PhaseName> phaseNameList, Scatterer scatterer,
                      boolean withAmp, SeismicSourceArgs sourceArgs, List<Arrival> arrivals) {
        super(modelName, depth, receiverDepth, phaseNameList, scatterer, withAmp, sourceArgs);
        this.arrivals = arrivals;
    }

    List<Arrival> arrivals;
}
