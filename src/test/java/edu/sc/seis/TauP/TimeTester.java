package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.ModelArgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Very simple replacement for running TauP_Time tool within older unit tests as refactor into app and lib
 * breaks dependency. Probably should eventually remove this.
 */
public class TimeTester {

    public TimeTester(String model) {
        modelArgs.setModelName(model);
    }

    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        for (SeismicPhase phase : phaseList) {
            List<Arrival> phaseArrivals = new ArrayList<>();
            for (RayCalculateable shoot : shootables) {
                phaseArrivals.addAll(shoot.calculate(phase));
            }
            arrivals.addAll(phaseArrivals);
        }
        Arrival.sortArrivals(arrivals);
        return arrivals;
    }



    public void setPhaseNames(List<String> phaseList) {
        phaseNameList = phaseList;
    }

    List<String> phaseNameList;

    public void setSourceDepth(double depth) {
        modelArgs.setSourceDepths(Collections.singletonList(depth));
    }

    ModelArgs modelArgs = new ModelArgs();

    public List<SeismicPhase> getSeismicPhases() throws TauModelException {
        double sourceDepth = modelArgs.getSourceDepths().get(0);
        double receiverDepth = modelArgs.getReceiverDepths().isEmpty() ? 0.0 : modelArgs.getReceiverDepths().get(0);
        TauModel tModDepth = modelArgs.depthCorrected(sourceDepth);
        boolean isDEBUG = false;
        List<SeismicPhase> newPhases = new ArrayList<>();
        for (String ph : phaseNameList) {
            newPhases.addAll(SeismicPhaseFactory.createSeismicPhases(
                    ph,
                    tModDepth,
                    sourceDepth,
                    receiverDepth,
                    modelArgs.getScatterer(),
                    isDEBUG));
        }
        return newPhases;
    }
}
