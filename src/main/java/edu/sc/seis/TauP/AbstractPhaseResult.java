package edu.sc.seis.TauP.gson;

import edu.sc.seis.TauP.PhaseName;
import edu.sc.seis.TauP.Scatterer;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;

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
                               SeismicSourceArgs sourceArgs) {
        this.model = model;
        this.sourcedepthlist = sourcedepthlist;
        this.receiverdepthlist = receiverdepthlist;
        this.phases = phases.stream().map(phaseName -> phaseName.getName()).collect(toList());;
        this.scatter = scatterer;
        if (withAmp) {
            this.sourceArg = sourceArgs;
        } else {
            this.sourceArg = null;
        }
    }

    String model;
    List<Double> sourcedepthlist;
    List<Double> receiverdepthlist;
    List<String> phases;
    Scatterer scatter;
    SeismicSourceArgs sourceArg;
}
