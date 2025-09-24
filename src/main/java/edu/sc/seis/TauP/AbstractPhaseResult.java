package edu.sc.seis.TauP;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class AbstractPhaseResult {

    public AbstractPhaseResult(String model,
                               List<Double> sourcedepthlist,
                               List<Double> receiverdepthlist,
                               List<PhaseName> phases,
                               Scatterer scatterer) {
        this(model, sourcedepthlist, receiverdepthlist, phases, scatterer, false, null);
    }

    public AbstractPhaseResult(String model,
                               List<Double> sourcedepthlist,
                               List<Double> receiverdepthlist,
                               List<PhaseName> phases,
                               Scatterer scatterer,
                               boolean withAmp,
                               SeismicSource sourceArgs) {
        this.model = model;
        this.sourcedepthlist = sourcedepthlist;
        this.receiverdepthlist = receiverdepthlist;
        this.phases = phases.stream().map(phaseName -> phaseName.getName()).collect(toList());
        this.scatter = scatterer;
        if (withAmp) {
            this.sourceArg = sourceArgs;
        } else {
            this.sourceArg = null;
        }
    }

    public String getModel() {
        return model;
    }

    public List<Double> getSourcedepthlist() {
        return sourcedepthlist;
    }

    public List<Double> getReceiverdepthlist() {
        return receiverdepthlist;
    }

    public List<String> getPhases() {
        return phases;
    }

    public Scatterer getScatter() {
        return scatter;
    }

    public SeismicSource getSourceArg() {
        return sourceArg;
    }

    String model;
    List<Double> sourcedepthlist;
    List<Double> receiverdepthlist;
    List<String> phases;
    Scatterer scatter;
    SeismicSource sourceArg;
}
