package edu.sc.seis.TauP;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class TauP_AbstractTimeTool extends TauP_Tool {
    public static final String DEFAULT_PHASES = "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS";
    protected String modelName = "iasp91";
    /**
     * Tau model calculated previously.
     *
     * @see TauModel
     */
    protected TauModel tMod;
    /**
     * TauModel derived from tMod by correcting it for a non-surface source.
     */
    protected transient TauModel tModDepth;
    /**
     * names of phases to be used, ie PKIKP.
     */
    protected List<PhaseName> phaseNames = new ArrayList<PhaseName>();
    protected double depth = 0.0;
    protected double receiverDepth = 0.0;
    protected double scattererDepth = 0.0;
    protected double scattererDistDeg = 0.0;
    /**
     * vector to hold the SeismicPhases for the phases named in phaseNames.
     */
    private List<SeismicPhase> phases = null;

    @Override
    public void init() throws TauPException {
        DEBUG = DEBUG || ToolRun.DEBUG;
        this.verbose = this.verbose || DEBUG || ToolRun.VERBOSE;

        if(phaseNames.size() == 0) {
            if(toolProps.containsKey("taup.phase.file")) {
                if(toolProps.containsKey("taup.phase.list")) {
                    parsePhaseList(toolProps.getProperty("taup.phase.list"));
                }
                try {
                    readPhaseFile(toolProps.getProperty("taup.phase.file"));
                } catch(IOException e) {
                    Alert.warning("Caught IOException while attempting to reading phase file "
                                    + toolProps.getProperty("taup.phase.file"),
                            e.getMessage());
                    if(phaseNames.size() <= 0) {
                        parsePhaseList(toolProps.getProperty("taup.phase.list",
                                DEFAULT_PHASES));
                    }
                }
            } else {
                parsePhaseList(toolProps.getProperty("taup.phase.list",
                        DEFAULT_PHASES));
            }
        }
        depth = Double.valueOf(toolProps.getProperty("taup.source.depth", "0.0"))
                .doubleValue();
        if(tMod == null
                || tMod.getVelocityModel().getModelName() != toolProps.getProperty("taup.model.name",
                "iasp91")) {
            modelName = toolProps.getProperty("taup.model.name", "iasp91");
            try {
                readTauModel();
            } catch(TauModelException ee) {
                if (ee.getCause() instanceof InvalidClassException) {
                    Alert.error("Model file "
                                    + modelName
                                    + " is not compatible with the current version.",
                            "Recreate using taup_create.");
                } else {
                    Alert.error("Caught TauModelException", ee.getMessage());
                }
                throw new RuntimeException(ee);
            }
        }
    }

    /**
     * Parse comma separated list of phase names, expanding convience phase names like
     * ttp into real phase names.
     *
     * @param phaseNames string to parse
     * @return parsed list of phase names
     */
    public static List<String> extractPhaseNames(String phaseNames) {
        List<String> names = new ArrayList<String>();
        for (String phaseName : TauP_AbstractTimeTool.splitPhaseNameList(phaseNames)) {
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
        out.put("sourcedepth", (float)depth);
        out.put("receiverdepth", (float)receiverDepth);
        JSONArray outPhases = new JSONArray();
        out.put("phases", outPhases);
        outPhases.putAll(phases);
        return out;
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
        Iterator<String> it = TauP_AbstractTimeTool.extractPhaseNames(phaseName).iterator();
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

    public double getSourceDepth() {
        return Double.valueOf(toolProps.getProperty("taup.source.depth", "0.0"))
                .doubleValue();
    }

    public void setSourceDepth(double depth) {
        this.depth = depth;
        toolProps.put("taup.source.depth", Double.toString(depth));
        clearPhases();
    }

    public double getReceiverDepth() {
        return receiverDepth;
    }

    public void setReceiverDepth(double receiverDepth) {
        if (this.receiverDepth != receiverDepth) {
            clearPhases();
        }
        this.receiverDepth = receiverDepth;
    }

    public double getScattererDepth() {
        return scattererDepth;
    }

    public void setScattererDepth(double depth) {
        if (this.getScattererDepth() != depth) {
            clearPhases();
        }
        this.scattererDepth = depth;
    }

    public double getScattererDistDeg() {
        return scattererDistDeg;
    }

    public void setScattererDistDeg(double distDeg) {
        if (distDeg > 180.0 || distDeg <= -180.0) {
            distDeg = (180.0 + distDeg) % 360.0 - 180.0;
        }
        if (distDeg == -180.0) {
            distDeg = 180;
        }
        if (this.getScattererDistDeg() != distDeg) {
            clearPhases();
        }
        this.scattererDistDeg = distDeg;
    }

    public void setScatterer(double depth, double degrees) {
        setScattererDepth(depth);
        setScattererDistDeg(degrees);
    }

    public String getTauModelName() {
        return modelName;
    }

    public TauModel getTauModel() {
        return tMod;
    }

    /**
     * Gets depth corrected TauModel.
     */
    public TauModel getTauModelDepthCorrected() throws TauModelException {
        if (tModDepth == null) {
            depthCorrect();
        }
        return tModDepth;
    }

    public void setTauModel(TauModel tMod) {
        clearPhases();
        this.tMod = tMod;
        this.tModDepth = null;
        modelName = tMod.getModelName();
        toolProps.put("taup.model.name", modelName);
        if (verbose) {
            Alert.info("Model set to " + tMod.getModelName()
                    + " with moho=" + tMod.getMohoDepth()
                    + " cmb=" + tMod.getCmbDepth()
                    + " iocb=" + tMod.getIocbDepth()
                    + " radius=" + tMod.getRadiusOfEarth());
        }
    }

    public void loadTauModel(String modelName) throws FileNotFoundException,
            InvalidClassException, IOException, StreamCorruptedException,
            OptionalDataException, TauModelException {
        this.modelName = modelName;
        readTauModel();
        this.modelName = tMod.getModelName();
    }

    public double[] getDisconDepths() {
        return tMod.getVelocityModel().getDisconDepths();
    }

    public void clearPhases() {
        phases = null;
    }

    public List<SeismicPhase> getSeismicPhases() {
        if (phases == null) {
            recalcPhases();
        }
        return Collections.unmodifiableList(phases);
    }

    /**
     * Reads the velocity model, slowness model, and tau model from a file saved
     * using Java's Serializable interface. Performs a depth correction if the
     * current depth is not 0.0
     */
    protected void readTauModel() throws TauModelException {
        TauModel tModLoad = TauModelLoader.load(modelName,
                toolProps.getProperty("taup.model.path"),
                verbose);
        if (tModLoad != null) {
            setTauModel(tModLoad);
        } else {
            throw new TauModelException("Unable to load " + modelName);
        }
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
        String[] namesInList = TauP_AbstractTimeTool.splitPhaseNameList(phaseList);
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

    /*
     * parses the standard command line args for the model, phase source and other
     * common items used by most tools. Does not do args related to distance or
     * output file.
     */
    protected String[] parseSourceModelCmdLineArgs(String[] origArgs) throws IOException {
        int i = 0;
        String[] args = parseCommonCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        boolean cmdLineArgPhase = false;
        boolean cmdLineArgPhaseFile = false;
        while (i < args.length) {
            if (i < args.length - 1) {
                if (dashEquals("mod", args[i]) || dashEquals("model", args[i])) {
                    toolProps.put("taup.model.name", args[i + 1]);
                    i++;
                } else if (args[i].equalsIgnoreCase("-h")) {
                    toolProps.put("taup.source.depth", args[i + 1]);
                    i++;
                } else if (args[i].equalsIgnoreCase("--stadepth")) {
                    setReceiverDepth(Double.parseDouble(args[i + 1]));
                    i++;
                } else if (i < args.length - 2 && (args[i].equalsIgnoreCase("--scat") || args[i].equalsIgnoreCase("--scatter"))) {
                    double scatterDepth = Double.valueOf(args[i + 1]).doubleValue();
                    double scatterDistDeg = Double.valueOf(args[i + 2]).doubleValue();
                    setScatterer(scatterDepth, scatterDistDeg);
                    i += 2;
                } else if (dashEquals("ph", args[i])) {
                    if (cmdLineArgPhase) {
                        // previous cmd line -ph so append
                        toolProps.put("taup.phase.list",
                                toolProps.getProperty("taup.phase.list",
                                        "")
                                        + "," + args[i + 1]);
                    } else {
                        // no previous cmd line -ph so replace defaults
                        toolProps.put("taup.phase.list", args[i + 1]);
                    }
                    cmdLineArgPhase = true;
                    i++;
                } else if (dashEquals("pf", args[i])) {
                    cmdLineArgPhaseFile = true;
                    toolProps.put("taup.phase.file", args[i + 1]);
                    i++;
                } else {
                    /*
                     * I don't know how to interpret this argument, so pass it
                     * back
                     */
                    noComprendoArgs[numNoComprendoArgs++] = args[i];
                }
            } else {
                /* I don't know how to interpret this argument, so pass it back */
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            }
            i++;
        }
        // check to see if there were phases or a phase file as an argument.
        // if so then dump the defaults
        if (cmdLineArgPhaseFile || cmdLineArgPhase) {
            if (cmdLineArgPhaseFile && !cmdLineArgPhase) {
                toolProps.remove("taup.phase.list");
            }
            if (!cmdLineArgPhaseFile && cmdLineArgPhase) {
                toolProps.remove("taup.phase.file");
            }
        }
        if (numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    /**
     * corrects the TauModel for the source, receiver and scatter depths.
     * In general, this is called by each tool's calculate methods, and so should
     * not need to be called by outside code. Most of the time calling setSourceDepth,
     * setReceiverDepth and setScatterDepth
     * is preferred, allowing the tool to choose when to call depthCorrect.
     */
    public void depthCorrect() throws TauModelException {
        depthCorrect(getSourceDepth(), getReceiverDepth(), getScattererDepth());
    }

    /**
     * corrects the TauModel for the given source depth. It only performs the
     * correction of the model is not already corrected to that depth.
     * In general, this is called by each tool's calculate methods, and so should
     * not need to be called by outside code. Most of the time calling setSourceDepth
     * is preferred, allowing the tool to choose when to call depthCorrect.
     *
     * @deprecated use setSourceDepth instead
     */
    @Deprecated
    public void depthCorrect(double depth) throws TauModelException {
        depthCorrect(depth, getReceiverDepth());
    }

    /**
     * In general, this is called by each tool's calculate methods, and so should
     * not need to be called by outside code. Most of the time calling setSourceDepth
     * and setReceiverDepth is preferred, allowing the tool to choose when to call depthCorrect.
     *
     * @param depth         the source depth
     * @param receiverDepth the receiver depth
     * @throws TauModelException
     */
    public void depthCorrect(double depth, double receiverDepth) throws TauModelException {
        depthCorrect(depth, receiverDepth, getScattererDepth());
    }

    /**
     * In general, this is called by each tool's calculate methods, and so should
     * not need to be called by outside code. Most of the time calling setSourceDepth
     * and setReceiverDepth and setScatterDepth is preferred, allowing the tool to choose when to call depthCorrect.
     *
     * @param depth         the source depth
     * @param receiverDepth the receiver depth
     * @param scatterDepth  scatterer depth, set to zero if no scattering
     * @throws TauModelException
     */
    public void depthCorrect(double depth, double receiverDepth, double scatterDepth) throws TauModelException {
        if (tModDepth == null || tModDepth.getSourceDepth() != depth) {
            setReceiverDepth(receiverDepth);
            tModDepth = tMod.depthCorrect(depth);
            tModDepth = tModDepth.splitBranch(receiverDepth);
            clearPhases();
        }
        if (!tModDepth.isBranchDepth(receiverDepth)) {
            setReceiverDepth(receiverDepth);
            tModDepth = tModDepth.splitBranch(receiverDepth); // if already split on receiver depth this does nothing
            clearPhases();
        }
        if (!tModDepth.isBranchDepth(scatterDepth)) {
            setScattererDepth(scatterDepth);
            tModDepth = tModDepth.splitBranch(scatterDepth); // if already split on scatter depth this does nothing
            clearPhases();
        }
        setSourceDepth(depth);
    }

    /**
     * recalculates the given phases using a possibly new or changed tau model.
     * This should not need to be called by outside classes as it is called by
     * depthCorrect, and calculate.
     */
    protected synchronized void recalcPhases() {
        List<SeismicPhase> newPhases = new ArrayList<SeismicPhase>();
        boolean alreadyAdded;
        String tempPhaseName;
        for (int phaseNameNum = 0; phaseNameNum < phaseNames.size(); phaseNameNum++) {
            tempPhaseName = phaseNames.get(phaseNameNum).getName();
            alreadyAdded = false;
            /*
            for(int phaseNum = 0; phaseNum < phases.size(); phaseNum++) {
                seismicPhase = phases.get(phaseNum);
                if(seismicPhase.name.equals(tempPhaseName)) {
                    phases.remove(phaseNum);
                    if(seismicPhase.sourceDepth == depth
                            && seismicPhase.tMod.equals(tModDepth)) {
                        // ok so copy to newPhases
                        newPhases.add(seismicPhase);
                        alreadyAdded = true;
                        if(verbose) {
                            Alert.info(seismicPhase.toString());
                        }
                        //break;
                    }
                }
            }
            */
            if (!alreadyAdded) {
                // didn't find it precomputed, so recalculate
                try {
                    List<SeismicPhase> calcPhaseList = SeismicPhaseFactory.createSeismicPhases(tempPhaseName, getTauModelDepthCorrected(), getSourceDepth(), getReceiverDepth(), getScattererDepth(), getScattererDistDeg(), DEBUG);
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
        }
        phases = newPhases;
    }

    public String getPhaseUsage() {
        return "-ph phase list     -- comma separated phase list\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + getModDepthUsage();
    }
}