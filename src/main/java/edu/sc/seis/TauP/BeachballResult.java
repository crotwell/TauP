package edu.sc.seis.TauP;

import java.util.List;

public class BeachballResult extends TimeResult {

    public BeachballResult(String model, List<Double> sourcedepthlist, List<Double> receiverdepthlist,
                           List<PhaseName> phases, Scatterer scatterer,
                           boolean withAmp, SeismicSource sourceArgs,
                           List<Arrival> arrivalList,
                           List<RadiationAmplitude> radPattern) {
        super(model, sourcedepthlist, receiverdepthlist, phases, scatterer, withAmp, sourceArgs, arrivalList);
        this.radPattern = radPattern;
    }

    public List<RadiationAmplitude> getRadiationPattern() {
        return radPattern;
    }

    List<RadiationAmplitude> radPattern;
}
