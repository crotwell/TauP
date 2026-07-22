package edu.sc.seis.TauP;

import java.util.List;

public class CurveResult extends AbstractPhaseResult {

    public CurveResult(String model, List<Double> sourcedepthlist, List<Double> receiverdepthlist,
                       List<PhaseName> phases, Scatterer scatterer,
                       XYPlotOutput xy) {
        super(model, sourcedepthlist, receiverdepthlist, phases, scatterer);
        this.xy = xy;
    }

    public XYPlotOutput getXY() {
        return xy;
    }

    XYPlotOutput xy;
}
