package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.ModelArgs;
import edu.sc.seis.TauP.CLI.Scatterer;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class TauP_AbstractPhaseTool extends TauP_Tool {
    public static final String DEFAULT_PHASES = "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS";

    public double getRadiusOfEarth() {
        try {
            return modelArgs.getTauModel().getRadiusOfEarth();
        } catch (TauModelException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * names of phases to be used, ie PKIKP.
     */
    protected List<PhaseName> phaseNames = new ArrayList<PhaseName>();

    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    /**
     * vector to hold the SeismicPhases for the phases named in phaseNames.
     */
    private List<SeismicPhase> phases = null;

    /**
     * Parse comma separated list of phase names, expanding convience phase names like
     * ttp into real phase names.
     *
     * @param phaseNames string to parse
     * @return parsed list of phase names
     */
    public static List<String> extractPhaseNames(String phaseNames) {
        List<String> names = new ArrayList<String>();
        for (String phaseName : TauP_AbstractPhaseTool.splitPhaseNameList(phaseNames)) {
            if (phaseName.equalsIgnoreCase("ttp")
                    || phaseName.equalsIgnoreCase("tts")
                    || phaseName.equalsIgnoreCase("ttbasic")
                    || phaseName.equalsIgnoreCase("tts+")
                    || phaseName.equalsIgnoreCase("ttp+")
                    || phaseName.equalsIgnoreCase("ttall")) {
                if (phaseName.equalsIgnoreCase("ttp")
                        || phaseName.equalsIgnoreCase("ttp+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("p");
                    names.add("P");
                    names.add("Pn");
                    names.add("Pdiff");
                    names.add("PKP");
                    names.add("PKiKP");
                    names.add("PKIKP");
                }
                if (phaseName.equalsIgnoreCase("tts")
                        || phaseName.equalsIgnoreCase("tts+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("s");
                    names.add("S");
                    names.add("Sn");
                    names.add("Sdiff");
                    names.add("SKS");
                    names.add("SKIKS");
                }
                if (phaseName.equalsIgnoreCase("ttp+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("PcP");
                    names.add("pP");
                    names.add("pPdiff");
                    names.add("pPKP");
                    names.add("pPKIKP");
                    names.add("pPKiKP");
                    names.add("sP");
                    names.add("sPdiff");
                    names.add("sPKP");
                    names.add("sPKIKP");
                    names.add("sPKiKP");
                }
                if (phaseName.equalsIgnoreCase("tts+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("sS");
                    names.add("sSdiff");
                    names.add("sSKS");
                    names.add("sSKIKS");
                    names.add("ScS");
                    names.add("pS");
                    names.add("pSdiff");
                    names.add("pSKS");
                    names.add("pSKIKS");
                }
                if (phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("ScP");
                    names.add("SKP");
                    names.add("SKIKP");
                    names.add("PKKP");
                    names.add("PKIKKIKP");
                    names.add("SKKP");
                    names.add("SKIKKIKP");
                    names.add("PP");
                    names.add("PKPPKP");
                    names.add("PKIKPPKIKP");
                }
                if (phaseName.equalsIgnoreCase("ttall")) {
                    names.add("SKiKP");
                    names.add("PP");
                    names.add("ScS");
                    names.add("PcS");
                    names.add("PKS");
                    names.add("PKIKS");
                    names.add("PKKS");
                    names.add("PKIKKIKS");
                    names.add("SKKS");
                    names.add("SKIKKIKS");
                    names.add("SKSSKS");
                    names.add("SKIKSSKIKS");
                    names.add("SS");
                    names.add("SP");
                    names.add("PS");
                }
            } else {
                names.add(phaseName);
            }
        }
        return names;
    }

    public static String[] splitPhaseNameList(String phaseList) {
        String phaseEntry = "";
        phaseList = phaseList.trim();
        phaseList = phaseList.replace(' ', ',');
        // remove any empty phases, ie two commas next to each other
        // should be replaced with one comma
        phaseList = phaseList.replaceAll(",,+", ",");
        // remove comma at beginning
        if (phaseList.startsWith(",")) {
            phaseList = phaseList.substring(1);
        }
        if (phaseList.length() == 0) {
            // phaseList is empty, no phases, so just return
            return new String[0];
        }
        // and comma at end
        if (phaseList.charAt(phaseList.length() - 1) == ',') {
            // we know that the length is > 1 as if not then we would have
            // returned from the previous if
            phaseList = phaseList.substring(0, phaseList.length() - 1);
        }
        String[] namesInList = phaseList.split(",");
        return namesInList;
    }

    /**
     * solves the equation (yb-ya)/(xb-xa) = (y-ya)/(x-xa) for y given x. Useful
     * for finding the pixel for a value given the dimension of the area and the
     * range of values it is supposed to cover. Note, this does not check for xa ==
     * xb, in which case a divide by zero would occur.
     */
    public static final double linearInterp(double xa,
                                            double ya,
                                            double xb,
                                            double yb,
                                            double x) {
        if (x == xa) {
            return ya;
        }
        if (x == xb) {
            return yb;
        }
        return (yb - ya) * (x - xa) / (xb - xa) + ya;
    }

    public static JSONObject baseResultAsJSONObject(String modelName,
                                                    double depth,
                                                    double receiverDepth,
                                                    String[] phases) {
        JSONObject out = new JSONObject();

        out.put("model", modelName);
        out.put("sourcedepth", (float) depth);
        out.put("receiverdepth", (float) receiverDepth);
        if (phases != null  ) {
            JSONArray outPhases = new JSONArray();
            out.put("phases", outPhases);
            outPhases.putAll(phases);
        }
        return out;
    }

    @Override
    public void init() throws TauPException {
        DEBUG = DEBUG || ToolRun.DEBUG;
        this.verbose = this.verbose || DEBUG || ToolRun.VERBOSE;

        if (phaseNames.size() == 0) {
            if (toolProps.containsKey("taup.phase.file")) {
                if (toolProps.containsKey("taup.phase.list")) {
                    parsePhaseList(toolProps.getProperty("taup.phase.list"));
                }
                try {
                    readPhaseFile(toolProps.getProperty("taup.phase.file"));
                } catch (IOException e) {
                    Alert.warning("Caught IOException while attempting to reading phase file "
                                    + toolProps.getProperty("taup.phase.file"),
                            e.getMessage());
                    if (phaseNames.size() <= 0) {
                        parsePhaseList(toolProps.getProperty("taup.phase.list",
                                DEFAULT_PHASES));
                    }
                }
            } else {
                parsePhaseList(toolProps.getProperty("taup.phase.list",
                        DEFAULT_PHASES));
            }
        }
    }

    /* Get/Set methods */
    public String[] getPhaseNames() {
        String[] phases = new String[phaseNames.size()];
        for (int i = 0; i < phaseNames.size(); i++) {
            phases[i] = phaseNames.get(i).getName();
        }
        return phases;
    }

    public String getPhaseNameString() {
        // in case of empty phase list
        if (getNumPhases() == 0)
            return "";
        String phases = phaseNames.get(0).getName();
        for (int i = 1; i < getNumPhases(); i++) {
            phases += "," + phaseNames.get(i).getName();
        }
        return phases;
    }

    @CommandLine.Option(names = {"-p", "--phase", "--ph"}, split = ",", description = "seismic phase names", defaultValue = DEFAULT_PHASES)
    public void setPhaseNames(String[] phaseNames) throws TauModelException {
        clearPhaseNames();
        for (int i = 0; i < phaseNames.length; i++) {
            appendPhaseName(phaseNames[i]);
        }
    }

    public void setPhaseNames(PhaseName[] phaseNames) {
        clearPhaseNames();
        for (int i = 0; i < phaseNames.length; i++) {
            this.phaseNames.add(phaseNames[i]);
        }
    }

    public synchronized void appendPhaseName(String phaseName)
            throws TauModelException {
        Iterator<String> it = TauP_AbstractPhaseTool.extractPhaseNames(phaseName).iterator();
        while (it.hasNext()) {
            appendPhaseName(new PhaseName(it.next()));
        }
    }

    public synchronized void appendPhaseName(PhaseName phaseName) {
        boolean unique = true;
        if (phaseName.name == null || phaseName.name.length() == 0) {
            // make sure not null string
            return;
        }
        for (int i = 0; i < phaseNames.size(); i++) {
            if (phaseNames.get(i).equals(phaseName)) {
                unique = false;
                return;
            }
        }
        if (unique) {
            this.phaseNames.add(phaseName);
        }
    }

    public int getNumPhases() {
        return phaseNames.size();
    }

    public void clearPhaseNames() {
        phases = null;
        phaseNames.clear();
    }

    @Deprecated
    public double getSourceDepth() {
        return this.modelArgs.getSourceDepth();
    }

    public void setSourceDepth(double depth) {
        this.modelArgs.setSourceDepth( depth);
        toolProps.put("taup.source.depth", Double.toString(depth));
        clearPhases();
    }

    public double getReceiverDepth() {
        return modelArgs.getReceiverDepth();
    }

    public void setReceiverDepth(double receiverDepth) {
        if (modelArgs.getReceiverDepth() != receiverDepth) {
            clearPhases();
        }
        modelArgs.setReceiverDepth(receiverDepth);
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
     */
    public TauModel getTauModelDepthCorrected() throws TauModelException {
        return modelArgs.depthCorrected();
    }

    public void setModelName(String modelName) {
        modelArgs.setModelName(modelName);
    }

    public void setTauModel(TauModel tMod) {
        clearPhases();
        modelArgs.setModelName(tMod.getModelName());
        this.modelArgs.setTMod(tMod);
        toolProps.put("taup.model.name", tMod.getModelName());
        if (verbose) {
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

    public List<SeismicPhase> getSeismicPhases() throws TauModelException {
        if (phases == null) {
            recalcPhases();
        }
        return Collections.unmodifiableList(phases);
    }

    /**
     * Reads in list of phase names from a text file. So long as each phase name
     * is separated by some whitespace, " " or newline or tab, it should read
     * them fine. Also, comments are allowed, either # or // are comments to the
     * end of the line while c style slash-star make a block a comment.
     */
    protected void readPhaseFile(String filename) throws IOException {
        FileReader fileIn = new FileReader(filename);
        StreamTokenizer tokenIn = new StreamTokenizer(fileIn);
        tokenIn.commentChar('#'); // '#' means ignore to end of line
        tokenIn.slashStarComments(true); // '/*...*/' means a comment
        tokenIn.slashSlashComments(true); // '//' means ignore to end of line
        tokenIn.wordChars('^', '^');
        tokenIn.wordChars('0', '9');
        tokenIn.wordChars('.', '.');
        tokenIn.wordChars('[', '[');
        tokenIn.wordChars(']', ']');
        while (tokenIn.nextToken() != StreamTokenizer.TT_EOF) {
            if (tokenIn.sval != null) {
                parsePhaseList(tokenIn.sval);
            } else {
                if (DEBUG) {
                    Alert.info("Token.sval was null! nval=" + tokenIn.nval);
                }
            }
        }
    }

    /**
     * parses a comma separated list of phase names and adds them to the
     * phaseNames vector. Each phase can have an optional argument after a dash.
     * This would be used for specifying which sac header to put the time in, or
     * for other unforeseen uses. This may be called multiple times to append
     * more phases. For example: P-0,PcP-1,ScP-4,Sn,SS,S^410S would, assuming no
     * previous phases have been added, put P in T0, PcP in T1, ScP in T5, Sn in
     * T2, SS in T3, and S^410S in T6.
     */
    public void parsePhaseList(String phaseList) {
        String phaseEntry = "";
        String[] namesInList = TauP_AbstractPhaseTool.splitPhaseNameList(phaseList);
        for (int i = 0; i < namesInList.length; i++) {
            String[] phaseAndHeader = namesInList[i].split("-");
            try {
                if (phaseAndHeader.length == 1) {
                    /* no optional dash argument, so just add the name. */
                    appendPhaseName(phaseAndHeader[0]);
                } else {
                    int startHeaderRange = -9;
                    int endHeaderRange = -9;
                    PhaseName sacPhase = new PhaseName(phaseAndHeader[0], phaseAndHeader[1]);
                    appendPhaseName(sacPhase);
                }
            } catch (TauModelException e) {
                Alert.warning("Problem with phase=" + phaseEntry + " "
                        + e.getMessage(), "Skipping this phase: ");
                if (verbose || DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * recalculates the given phases using a possibly new or changed tau model.
     * This should not need to be called by outside classes as it is called by
     * depthCorrect, and calculate.
     */
    protected synchronized void recalcPhases() throws TauModelException {
        List<SeismicPhase> newPhases = new ArrayList<SeismicPhase>();
        TauModel tModDepth = modelArgs.depthCorrected();
        for (PhaseName phaseName : phaseNames) {
            String tempPhaseName = phaseName.getName();
            // didn't find it precomputed, so recalculate
            try {
                List<SeismicPhase> calcPhaseList = SeismicPhaseFactory.createSeismicPhases(
                        phaseName.getName(),
                        tModDepth,
                        modelArgs.getSourceDepth(),
                        modelArgs.getReceiverDepth(),
                        modelArgs.getScatterer(),
                        DEBUG);
                newPhases.addAll(calcPhaseList);
                for (SeismicPhase seismicPhase : newPhases) {
                    if (verbose) {
                        Alert.info(seismicPhase.toString());
                    }
                }
            } catch (ScatterArrivalFailException e) {
                Alert.warning(e.getMessage() + ", skipping this phase");
                if (verbose || DEBUG) {
                    e.printStackTrace();
                }
            } catch (TauModelException e) {
                Alert.warning("Error with phase=" + tempPhaseName,
                        e.getMessage() + "\nSkipping this phase");
                if (verbose || DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                if (verbose) {
                    Alert.info("-----------------");
                }
            }
        }
        phases = newPhases;
    }

    public String getPhaseUsage() {
        return "-ph phase list     -- comma separated phase list\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + getModDepthUsage();
    }
}
