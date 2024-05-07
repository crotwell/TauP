package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cli.ModelArgs;
import edu.sc.seis.TauP.cli.Scatterer;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.*;

public abstract class TauP_AbstractPhaseTool extends TauP_Tool {
    public static final String DEFAULT_PHASES = "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS";

    public TauP_AbstractPhaseTool(AbstractOutputTypeArgs outputTypeArgs) {
        super(outputTypeArgs);
        phaseArgs.tool = this;
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
            if (phaseArgs.isEmpty()) {
                for (String pStr : extractPhaseNames(DEFAULT_PHASES)) {
                    appendPhaseName(pStr);
                }
            }
            this.phaseNames = new ArrayList<>();
            for (String pStr : phaseArgs.phaseNames) {
                appendPhaseName(pStr);
            }
            for (String filename : phaseArgs.phaseFileList) {
                List<String> pList = null;
                try {
                    pList = readPhaseFile(filename);
                } catch (IOException e) {
                    throw new PhaseParseException("Unable to parse file: "+filename, e);
                }
                for (String pStr : pList) {
                    appendPhaseName(pStr);
                }
            }
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

    /**
     * Parse comma separated list of phase names, expanding convience phase names like
     * ttp into real phase names.
     *
     * @param phaseNames string to parse
     * @return parsed list of phase names
     */
    public static List<String> extractPhaseNames(String phaseNames) {
        List<String> names = new ArrayList<>();
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
        phaseList = phaseList.trim();
        phaseList = phaseList.replace(' ', ',');
        // remove any empty phases, ie two commas next to each other
        // should be replaced with one comma
        phaseList = phaseList.replaceAll(",,+", ",");
        // remove comma at beginning
        if (phaseList.startsWith(",")) {
            phaseList = phaseList.substring(1);
        }
        if (phaseList.isEmpty()) {
            // phaseList is empty, no phases, so just return
            return new String[0];
        }
        // and comma at end
        if (phaseList.charAt(phaseList.length() - 1) == ',') {
            // we know that the length is > 1 as if not then we would have
            // returned from the previous if
            phaseList = phaseList.substring(0, phaseList.length() - 1);
        }
        return phaseList.split(",");
    }

    /**
     * solves the equation (yb-ya)/(xb-xa) = (y-ya)/(x-xa) for y given x. Useful
     * for finding the pixel for a value given the dimension of the area and the
     * range of values it is supposed to cover. Note, this does not check for xa ==
     * xb, in which case a divide by zero would occur.
     */
    public static double linearInterp(double xa,
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

    }

    /* Get/Set methods */
    @Deprecated
    public String[] getPhaseNames() {
        try {
            String[] phases = new String[parsePhaseNameList().size()];
            for (int i = 0; i < parsePhaseNameList().size(); i++) {
                phases[i] = parsePhaseNameList().get(i).getName();
            }
            return phases;
        } catch (PhaseParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public String getPhaseNameString() {
        try {
        // in case of empty phase list
        if (getNumPhases() == 0)
            return "";
        String phases = parsePhaseNameList().get(0).getName();
        for (int i = 1; i < getNumPhases(); i++) {
            phases += "," + parsePhaseNameList().get(i).getName();
        }
        return phases;
        } catch (PhaseParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPhaseNames(List<String> phaseNames) throws PhaseParseException {
        clearPhaseNames();
        for (String phasename : phaseNames) {
            appendPhaseName(phasename);
        }
    }

    @Deprecated
    public void setPhaseNames(PhaseName[] phaseNames) {
        this.setPhaseNameList(Arrays.asList(phaseNames));
    }

    public synchronized void appendPhaseName(String phaseName)
            throws PhaseParseException {
        Iterator<String> it = TauP_AbstractPhaseTool.extractPhaseNames(phaseName).iterator();
        while (it.hasNext()) {
            appendPhaseName(PhaseName.parseName(it.next()));
        }
    }

    public synchronized void appendPhaseName(PhaseName phaseName) throws PhaseParseException {
        boolean unique = true;
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
     *
     * @return
     */
    protected List<String> readPhaseFile(String filename) throws IOException {
        List<String> out = new ArrayList<>();
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
                out.add(tokenIn.sval);
            } else {
                if (isDEBUG()) {
                    Alert.info("Token.sval was null! nval=" + tokenIn.nval);
                }
            }
        }
        return out;
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
     * @return
     */
    public List<PhaseName> parsePhaseList(String phaseList) {
        List<PhaseName> out = new ArrayList<>();
        String phaseEntry = "";
        String[] namesInList = TauP_AbstractPhaseTool.splitPhaseNameList(phaseList);
        for (int i = 0; i < namesInList.length; i++) {
            try {
                out.add(PhaseName.parseName(namesInList[i]));
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

    /**
     * recalculates the given phases using a possibly new or changed tau model.
     * This should not need to be called by outside classes as it is called by
     * depthCorrect, and calculate.
     */
    protected synchronized void recalcPhases() throws TauModelException {
        List<SeismicPhase> newPhases = new ArrayList<>();
        TauModel tModDepth = modelArgs.depthCorrected();
        for (PhaseName phaseName : parsePhaseNameList()) {
            String tempPhaseName = phaseName.getName();
            // didn't find it precomputed, so recalculate
            try {
                List<SeismicPhase> calcPhaseList = SeismicPhaseFactory.createSeismicPhases(
                        phaseName.getName(),
                        tModDepth,
                        modelArgs.getSourceDepth(),
                        modelArgs.getReceiverDepth(),
                        modelArgs.getScatterer(),
                        isDEBUG());
                newPhases.addAll(calcPhaseList);
                for (SeismicPhase seismicPhase : newPhases) {
                    if (isVerbose()) {
                        Alert.info(seismicPhase.toString());
                    }
                }
            } catch (ScatterArrivalFailException e) {
                Alert.warning(e.getMessage() + ", skipping this phase");
                if (isVerbose() || isDEBUG()) {
                    e.printStackTrace();
                }
            } catch (TauModelException e) {
                Alert.warning("Error with phase=" + tempPhaseName,
                        e.getMessage() + "\nSkipping this phase");
                if (isVerbose() || isDEBUG()) {
                    e.printStackTrace();
                }
            } finally {
                if (isVerbose()) {
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


    @CommandLine.ArgGroup(heading = "Phase Names %n", exclusive = false)
    PhaseArgsInner phaseArgs = new PhaseArgsInner();

    static class PhaseArgsInner {


        TauP_AbstractPhaseTool tool;

        List<String> phaseNames = new ArrayList<>();

        /**
         * names of phases to be used, ie PKIKP.
         */
        @CommandLine.Option(names = {"-p", "--phase", "--ph"}, split = ",", description = "seismic phase names")
        public void setPhaseNames(List<String> phaseNamesStr) throws PhaseParseException {
            if (tool != null) {
                tool.clearPhaseNames();
            }
            for (String phArg : phaseNamesStr) {
                for (String ph : extractPhaseNames(phArg)) {
                    boolean found = false;
                    for (String prev : phaseNames) {
                        if (prev.equals(ph)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        phaseNames.add(ph);
                    }
                }
            }
        };


        @CommandLine.Option(names = "--phasefile", description = "read list of phase names from file")
        public void setPhaseFiles(List<String> phaseFile) {
            if (tool != null) {
                tool.clearPhaseNames();
            }
            phaseFileList = phaseFile;
        }
        List<String> phaseFileList = new ArrayList<>();

        public boolean isEmpty() {
            return phaseNames.isEmpty() && phaseFileList.isEmpty();
        }
    }

}
