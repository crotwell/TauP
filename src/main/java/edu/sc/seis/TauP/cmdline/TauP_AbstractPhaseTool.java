package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.ModelArgs;
import edu.sc.seis.TauP.Scatterer;
import edu.sc.seis.TauP.cmdline.args.PhaseArgs;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.*;

public abstract class TauP_AbstractPhaseTool extends TauP_Tool {

    public TauP_AbstractPhaseTool(AbstractOutputTypeArgs outputTypeArgs) {
        super(outputTypeArgs);
        phaseArgs.setTool(this);
    }

    public double getRadiusOfEarth() {
        try {
            return modelArgs.getTauModel().getRadiusOfEarth();
        } catch (TauModelException e) {
            throw new RuntimeException(e);
        }
    }

    public List<PhaseName> parsePhaseNameList() throws PhaseParseException {
        if (this.phaseNames == null) {
            this.phaseNames = phaseArgs.parsePhaseNameList();
        }
        return this.phaseNames;
    }

    public void setPhaseNameList(List<PhaseName> phaseNames) {
        clearPhases();
        this.phaseNames = phaseNames;
    }
    /**
     * names of phases to be used, ie PKIKP.
     */
    protected List<PhaseName> phaseNames = null;

    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    /**
     * vector to hold the SeismicPhases for the phases named in phaseNames.
     */
    private List<SeismicPhase> phases = null;

    public static JSONObject baseResultAsJSONObject(String modelName,
                                                    List<Double> depth,
                                                    List<Double> receiverDepth,
                                                    List<PhaseName> phaseNameList) {
        JSONObject out = new JSONObject();

        out.put("model", modelName);
        out.put("sourcedepth",  depth);
        out.put("receiverdepth", receiverDepth);
        if (phaseNameList != null  ) {
            JSONArray outPhases = new JSONArray();
            for (PhaseName pn : phaseNameList) {
                outPhases.put(pn.getName());
            }
            out.put("phases", outPhases);
        }
        return out;
    }

    @Override
    public void init() throws TauPException {
        // no op
    }

    /* Get/Set methods */

    public void setPhaseNames(List<String> phaseNames) throws PhaseParseException {
        clearPhaseNames();
        for (String phasename : phaseNames) {
            appendPhaseName(phasename);
        }
    }

    public synchronized void appendPhaseName(String phaseName)
            throws PhaseParseException {
        for (String s : PhaseArgs.extractPhaseNames(phaseName)) {
            appendPhaseName(PhaseName.parseName(s));
        }
    }

    public synchronized void appendPhaseName(PhaseName phaseName) {
        if (phaseName.name == null || phaseName.name.isEmpty()) {
            // make sure not null string
            return;
        }
        this.phaseNames.add(phaseName);
    }

    public int getNumPhases() {
        try {
            return parsePhaseNameList().size();
        } catch (PhaseParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearPhaseNames() {
        phases = null;
        phaseNames = new ArrayList<>();
    }

    public void setSingleSourceDepth(double depth) {
        this.modelArgs.setSourceDepths( Collections.singletonList(depth));
        toolProps.put("taup.source.depth", Double.toString(depth));
        clearPhases();
    }

    public void setReceiverDepth(double receiverDepth) {

    }
    public void setSingleReceiverDepth(double receiverDepth) {
        this.modelArgs.setReceiverDepths(Collections.singletonList(receiverDepth));
        clearPhases();
    }

    public double getScattererDepth() {
        return modelArgs.getScatterer().depth;
    }

    public Scatterer getScatterer() {
        return modelArgs.getScatterer();
    }

    public void setScatterer(Scatterer scatterer) {
        clearPhases();
        modelArgs.setScatterer(scatterer.depth, scatterer.dist.getDegrees(getRadiusOfEarth()));
    }

    public String getTauModelName() {
        return modelArgs.getModelName();
    }

    /**
     * Gets depth corrected TauModel.
     *
     * @param sourceDepth earthqauke source depth
     * @throws TauModelException if depth correct fails
     * @return corrected tau model
     */
    public TauModel getTauModelDepthCorrected(double sourceDepth) throws TauModelException {
        return modelArgs.depthCorrected(sourceDepth);
    }

    public void setModelName(String modelName) {
        modelArgs.setModelName(modelName);
    }

    public void setTauModel(TauModel tMod) {
        clearPhases();
        modelArgs.setModelName(tMod.getModelName());
        this.modelArgs.setTMod(tMod);
        toolProps.put("taup.model.name", tMod.getModelName());
        if (isVerbose()) {
            Alert.info("Model set to " + tMod.getModelName()
                    + " with moho=" + tMod.getMohoDepth()
                    + " cmb=" + tMod.getCmbDepth()
                    + " iocb=" + tMod.getIocbDepth()
                    + " radius=" + tMod.getRadiusOfEarth());
        }
    }

    public void clearPhases() {
        phases = null;
    }

    /**
     * Calculates the seismic phases using a possibly new or changed tau model for the given source depth.
     *
     * @param sourceDepth earthquake source depth
     * @throws TauModelException if calculation fails
     * @return calculated seismic phases
     */
    public List<SeismicPhase> calcSeismicPhases(double sourceDepth) throws TauPException {
        return calcSeismicPhases(sourceDepth, getReceiverDepths(), modelArgs.getScatterer());
    }
    public List<SeismicPhase> calcSeismicPhases(double sourceDepth, List<Double> receiverDepths, Scatterer scatterer) throws TauModelException {
        return SeismicPhaseFactory.calculateSeismicPhases(modelArgs.depthCorrected(sourceDepth), parsePhaseNameList(), sourceDepth, receiverDepths, scatterer);
    }

    public List<Double> getSourceDepths() throws TauPException {
        List<Double> out = new ArrayList<>();
        out.addAll(modelArgs.getSourceDepths());
        if ( out.isEmpty()) {
            out.add(Double.parseDouble(toolProps.getProperty("taup.source.depth", "0.0")));
        }
        return out;
    }

    public List<Double> getReceiverDepths() throws TauPException {
        List<Double> out = modelArgs.getReceiverDepths();
        if (out.isEmpty()) {
            // default if not receiver depths given is surface
            out.add(0.0);
        }
        return out;
    }

    public List<SeismicPhase> getSeismicPhases() throws TauPException {
        if (phases == null) {
            phases = new ArrayList<>();
            List<Double> receiverDepths = getReceiverDepths();
            for (Double sourceDepth : getSourceDepths()) {
                phases.addAll(calcSeismicPhases(sourceDepth, receiverDepths, modelArgs.getScatterer()));
            }
        }
        return phases;
    }


    /**
     * parses a comma separated list of phase names and adds them to the
     * phaseNames vector. Each phase can have an optional argument after a dash.
     * This would be used for specifying which sac header to put the time in, or
     * for other unforeseen uses. This may be called multiple times to append
     * more phases. For example: P-0,PcP-1,ScP-4,Sn,SS,S^410S would, assuming no
     * previous phases have been added, put P in T0, PcP in T1, ScP in T5, Sn in
     * T2, SS in T3, and S^410S in T6.
     *
     * @param phaseList comma separates list of phase names
     * @return list of phases
     */
    public List<PhaseName> parsePhaseList(String phaseList) {
        List<PhaseName> out = new ArrayList<>();
        String phaseEntry = "";
        String[] namesInList = PhaseArgs.splitPhaseNameList(phaseList);
        for (String s : namesInList) {
            try {
                out.add(PhaseName.parseName(s));
            } catch (TauModelException e) {
                Alert.warning("Problem with phase=" + phaseEntry + " "
                        + e.getMessage(), "Skipping this phase: ");
                if (isVerbose() || isDEBUG()) {
                    e.printStackTrace();
                }
            }
        }
        return out;
    }


    @CommandLine.ArgGroup(heading = "Phase Names %n", exclusive = false)
    PhaseArgs phaseArgs = new PhaseArgs();

}
