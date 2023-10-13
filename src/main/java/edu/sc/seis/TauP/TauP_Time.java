/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place -
 * Suite 330, Boston, MA 02111-1307, USA. The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu </A> Bug reports and comments
 * should be directed to H. Philip Crotwell, crotwell@seis.sc.edu or Tom Owens,
 * owens@seis.sc.edu
 */
package edu.sc.seis.TauP;

import java.io.*;
import java.util.*;

/**
 * Calculate travel times for different branches using linear interpolation
 * between known slowness samples.
 *
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
public class TauP_Time extends TauP_Tool {


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
     * vector to hold the SeismicPhases for the phases named in phaseNames.
     */
    private List<SeismicPhase> phases = null;

    /** names of phases to be used, ie PKIKP. */
    protected List<PhaseName> phaseNames = new ArrayList<PhaseName>();

    protected double depth = 0.0;

    protected double receiverDepth = 0.0;

    protected List<Double> degreesList = new ArrayList<Double>();

    protected double scattererDepth = 0.0;
    protected double scattererDistDeg = 0.0;

    /**
     * For when command line args uses --km for distance. Have to wait until
     * after the model is read in to get radius of earth.
     */
    protected List<Double> distKilometersList = new ArrayList<Double>();

    protected double azimuth = Double.MAX_VALUE;

    protected double backAzimuth = Double.MAX_VALUE;

    protected double takeoffAngle = Double.MAX_VALUE;

    protected double shootRayp = Double.MAX_VALUE;

    protected double stationLat = Double.MAX_VALUE;

    protected double stationLon = Double.MAX_VALUE;

    protected double eventLat = Double.MAX_VALUE;

    protected double eventLon = Double.MAX_VALUE;

    protected List<Arrival> arrivals = new ArrayList<Arrival>();

    protected boolean GUI = false;

    protected boolean onlyPrintRayP = false;

    protected boolean onlyPrintTime = false;

    protected boolean onlyFirst = false;

    protected String relativePhaseName = "";

    public static final String DEFAULT_PHASES = "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS";

    public TauP_Time() {
    }

    public TauP_Time(TauModel tMod)  {
        setTauModel(tMod);
    }

    /**
     * creates a TauP_Time object with the tau model specified by modelName
     * already loaded.
     *
     * @throws TauModelException
     *             if the file can't be found or is corrupted in some way.
     */
    public TauP_Time(String modelName) throws TauModelException {
        try {
            loadTauModel(modelName);
        } catch(FileNotFoundException e) {
            throw new TauModelException("FileNotFoundException:"
                    + e.getMessage(), e);
        } catch(InvalidClassException e) {
            throw new TauModelException("InvalidClassException:"
                    + e.getMessage(), e);
        } catch(StreamCorruptedException e) {
            throw new TauModelException("StreamCorruptedException:"
                    + e.getMessage(), e);
        } catch(OptionalDataException e) {
            throw new TauModelException("OptionalDataException:"
                    + e.getMessage(), e);
        } catch(IOException e) {
            throw new TauModelException("IOException:" + e.getMessage(), e);
        }
    }

    @Override
    public String[] allowedOutputFormats() {
        String[] formats = {TEXT, JSON};
        return formats;
    }

    /* Get/Set methods */
    public String[] getPhaseNames() {
        String[] phases = new String[phaseNames.size()];
        for(int i = 0; i < phaseNames.size(); i++) {
            phases[i] = phaseNames.get(i).getName();
        }
        return phases;
    }

    public String getPhaseNameString() {
        // in case of empty phase list
        if(getNumPhases() == 0)
            return "";
        String phases = phaseNames.get(0).getName();
        for(int i = 1; i < getNumPhases(); i++) {
            phases += "," + phaseNames.get(i).getName();
        }
        return phases;
    }

    public void setPhaseNames(String[] phaseNames) throws TauModelException {
        clearPhaseNames();
        for(int i = 0; i < phaseNames.length; i++) {
            appendPhaseName(phaseNames[i]);
        }
    }

    public void setPhaseNames(PhaseName[] phaseNames) {
        clearPhaseNames();
        for(int i = 0; i < phaseNames.length; i++) {
            this.phaseNames.add(phaseNames[i]);
        }
    }

    /**
     * @deprecated see extractPhaseNames
     * @param phaseName
     * @return
     */
    public static List<String> getPhaseNames(String phaseName) {
        return extractPhaseNames(phaseName);
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
        for (String phaseName : splitPhaseNameList(phaseNames)) {
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

    public synchronized void appendPhaseName(String phaseName)
            throws TauModelException {
        Iterator<String> it = extractPhaseNames(phaseName).iterator();
        while(it.hasNext()) {
            appendPhaseName(new PhaseName(it.next()));
        }
    }

    public synchronized void appendPhaseName(PhaseName phaseName) {
        boolean unique = true;
        if(phaseName.name == null || phaseName.name.length() == 0) {
            // make sure not null string
            return;
        }
        for(int i = 0; i < phaseNames.size(); i++) {
            if(phaseNames.get(i).equals(phaseName)) {
                unique = false;
                return;
            }
        }
        if(unique) {
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
        if (distDeg == -180.0) { distDeg = 180; }
        if (this.getScattererDistDeg() != distDeg) {
            clearPhases();
        }
        this.scattererDistDeg = distDeg ;
    }

    public void setScatterer(double depth, double degrees) {
        setScattererDepth(depth);
        setScattererDistDeg(degrees);
    }

    public void setEventLatLon(double lat, double lon) {
        this.eventLat = lat;
        this.eventLon = lon;
    }

    public void setStationLatLon(double lat, double lon) {
        this.stationLat = lat;
        this.stationLon = lon;
    }

    public String getTauModelName() {
        return modelName;
    }

    public TauModel getTauModel() {
        return tMod;
    }

    /** Gets depth corrected TauModel. */
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
            Alert.info("Model set to "+tMod.getModelName()
            +" with moho="+tMod.getMohoDepth()
            +" cmb="+tMod.getCmbDepth()
            +" iocb="+tMod.getIocbDepth()
            +" radius="+tMod.getRadiusOfEarth());
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
        clearArrivals();
        phases = null;
    }

    public void clearArrivals() {
        arrivals = new ArrayList<Arrival>();
    }

    public int getNumArrivals() {
        return arrivals.size();
    }

    public Arrival getArrival(int i) {
        return (Arrival)arrivals.get(i);
    }

    public List<Arrival> getArrivals() {
        return Collections.unmodifiableList(arrivals);
    }

    public List<SeismicPhase> getSeismicPhases() {
        if (phases == null) {
            recalcPhases();
        }
        return Collections.unmodifiableList(phases);
    }

    /* Normal methods */
    /**
     * Reads the velocity model, slowness model, and tau model from a file saved
     * using Java's Serializable interface. Performs a depth correction if the
     * current depth is not 0.0
     */
    protected void readTauModel() throws TauModelException {
            TauModel tModLoad = TauModelLoader.load(modelName,
                                                    toolProps.getProperty("taup.model.path"),
                                                    verbose);
            if(tModLoad != null) {
                setTauModel(tModLoad);
            } else {
                throw new TauModelException("Unable to load "+modelName);
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
        while(tokenIn.nextToken() != StreamTokenizer.TT_EOF) {
            if(tokenIn.sval != null) {
                parsePhaseList(tokenIn.sval);
            } else {
                if(DEBUG) {
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
        String[] namesInList = splitPhaseNameList(phaseList);
        for(int i = 0; i < namesInList.length; i++) {
            String[] phaseAndHeader = namesInList[i].split("-");
            try {
                if(phaseAndHeader.length == 1) {
                    /* no optional dash argument, so just add the name. */
                    appendPhaseName(phaseAndHeader[0]);
                } else {
                    int startHeaderRange = -9;
                    int endHeaderRange = -9;
                    PhaseName sacPhase = new PhaseName(phaseAndHeader[0], phaseAndHeader[1]);
                    appendPhaseName(sacPhase);
                }
            } catch(TauModelException e) {
                Alert.warning("Problem with phase=" + phaseEntry + " "
                        + e.getMessage(), "Skipping this phase: ");
                if (verbose || DEBUG) {
                    e.printStackTrace();
                }
            }
        }
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
     * Parses a comma separated list of distances and returns them in an array.
     */
    public static List<Double> parseDegreeList(String degList) {
        degList = degList.trim();
        while (degList.startsWith(",")) {
            degList = degList.substring(1);
        }
        while(degList.endsWith(",")) {
            degList = degList.substring(0, degList.length()-1);
        }
        String[] split = degList.trim().split(",");
        List<Double> degreesFound = new ArrayList<Double>(split.length);
        for (int i = 0; i < split.length; i++) {
            try {
                degreesFound.add(Double.parseDouble(split[i].trim()));
            } catch (NumberFormatException e) {
                // oh well
                System.err.println("can't parse '"+split[i]+"' as number, skipping.");
            }
        }
        return degreesFound;
    }

    /*
     * parses the standard command line args for the model, phase source and other
     * common items used by most tools. Does not do args related to distance or
     * output file.
     */
    protected String[] parseSourceModelCmdLineArgs(String[] origArgs) throws IOException {
        int i = 0;
        String[] args = super.parseCommonCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        boolean cmdLineArgPhase = false;
        boolean cmdLineArgPhaseFile = false;
        while(i < args.length) {
            if(i < args.length - 1) {
                if(dashEquals("mod", args[i]) || dashEquals("model", args[i])) {
                    toolProps.put("taup.model.name", args[i + 1]);
                    i++;
                } else if(args[i].equalsIgnoreCase("-h")) {
                    toolProps.put("taup.source.depth", args[i + 1]);
                    i++;
                } else if(args[i].equalsIgnoreCase("--stadepth")) {
                    setReceiverDepth(Double.parseDouble(args[i + 1]));
                    i++;
                } else if(i < args.length - 2 && (args[i].equalsIgnoreCase("--scat") || args[i].equalsIgnoreCase("--scatter"))) {
                    double scatterDepth = Double.valueOf(args[i + 1]).doubleValue();
                    double scatterDistDeg = Double.valueOf(args[i + 2]).doubleValue();
                    setScatterer(scatterDepth, scatterDistDeg);
                    i += 2;
                } else if(dashEquals("ph", args[i])) {
                    if(cmdLineArgPhase) {
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
                } else if(dashEquals("pf", args[i])) {
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
        if(cmdLineArgPhaseFile || cmdLineArgPhase) {
            if(cmdLineArgPhaseFile && !cmdLineArgPhase) {
                toolProps.remove("taup.phase.list");
            }
            if(!cmdLineArgPhaseFile && cmdLineArgPhase) {
                toolProps.remove("taup.phase.file");
            }
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    /*
     * parses the standard command line args for the taup package. Other tools
     * that subclass this class will likely override this.
     */
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        int i = 0;
        String[] args = parseSourceModelCmdLineArgs(origArgs);
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        boolean cmdLineArgPhase = false;
        boolean cmdLineArgPhaseFile = false;
        while(i < args.length) {
            if(dashEquals("json", args[i])) {
                outputFormat = TauP_Tool.JSON;
            } else if(dashEquals("gui", args[i])) {
                GUI = true;
            } else if(dashEquals("rayp", args[i])) {
                onlyPrintRayP = true;
                onlyPrintTime = false;
            } else if(dashEquals("time", args[i])) {
                onlyPrintTime = true;
                onlyPrintRayP = false;
            } else if(dashEquals("first", args[i])) {
                onlyFirst = true;
            } else if(i < args.length - 1) {
                if(dashEquals("deg", args[i])) {
                    degreesList = parseDegreeList(args[i+1]);
                    i++;
                } else if(dashEquals("km", args[i])) {
                    distKilometersList = parseDegreeList(args[i+1]);
                    //distKilometersList.add(Double.valueOf(args[i + 1]).doubleValue());
                    i++;
                } else if(dashEquals("az", args[i])) {
                    azimuth = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("baz", args[i])) {
                    backAzimuth = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("takeoff", args[i])) {
                    takeoffAngle = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("shootray", args[i])) {
                    shootRayp = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(dashEquals("rel", args[i])) {
                    relativePhaseName = args[i + 1];
                    i++;
                } else if(i < args.length - 2) {
                    if(args[i].equalsIgnoreCase("-sta")
                            || args[i].equalsIgnoreCase("-station")) {
                        setStationLatLon(Double.valueOf(args[i + 1]).doubleValue(),
                                         Double.valueOf(args[i + 2]).doubleValue());
                        i += 2;
                    } else if(args[i].equalsIgnoreCase("-evt")
                            || args[i].equalsIgnoreCase("-event")) {
                        setEventLatLon( Double.valueOf(args[i + 1]).doubleValue(),
                                        Double.valueOf(args[i + 2]).doubleValue());
                        i += 2;
                    } else {
                        /*
                         * I don't know how to interpret this argument, so pass
                         * it back
                         */
                        noComprendoArgs[numNoComprendoArgs++] = args[i];
                    }
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
        if(cmdLineArgPhaseFile || cmdLineArgPhase) {
            if(cmdLineArgPhaseFile && !cmdLineArgPhase) {
                toolProps.remove("taup.phase.list");
            }
            if(!cmdLineArgPhaseFile && cmdLineArgPhase) {
                toolProps.remove("taup.phase.file");
            }
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    public synchronized List<Arrival> sortArrivals(List<Arrival> arrivals) {
        Collections.sort(arrivals, new Comparator<Arrival>() {
            public int compare(Arrival o1, Arrival o2) {
                return Double.compare(o1.getTime(), o2.getTime());
            }
        });
        return arrivals;
    }

    public List<Arrival> calculate(double degrees) throws TauModelException {
        List<Double> dList = Arrays.asList(new Double[] {degrees});
        return calculate(dList);
    }

    public List<Arrival> calculate(List<Double> degreesList) throws TauModelException {
        List<Arrival> arrivals = calcTime(degreesList);
        return sortArrivals(arrivals);
    }

    public List<Arrival> calcEventStation(Double[] evloc, List<Double[]> staloc) throws TauModelException {
        setEventLatLon(evloc[0], evloc[1]);
        List<Arrival> out = new ArrayList<>();
        for (Double[] sta : staloc) {
            clearArrivals();
            setEventLatLon(evloc[0], evloc[1]);
            setStationLatLon(sta[0], sta[1]);
            degreesList.add(SphericalCoords.distance(sta[0], sta[1], evloc[0], evloc[1]));
            azimuth = SphericalCoords.azimuth(evloc[0], evloc[1], sta[0], sta[1]);
            backAzimuth = SphericalCoords.azimuth(sta[0], sta[1], evloc[0], evloc[1]);
            out.addAll(calculate(degreesList));
        }
        this.arrivals = sortArrivals(out);
        return this.arrivals;
    }

    public Arrival calculateRelativeArrival(double degrees) throws TauModelException {
        Arrival relativeArrival = null;
        if (relativePhaseName != "") {
            List<SeismicPhase> relPhases = new ArrayList<SeismicPhase>();
            List<String> splitNames = extractPhaseNames(relativePhaseName);
            for (String sName : splitNames) {
                try {
                    List<SeismicPhase> calcRelPhaseList = SeismicPhaseFactory.createSeismicPhases(
                            sName,
                            getTauModelDepthCorrected(),
                            this.getSourceDepth(),
                            this.getReceiverDepth(),
                            this.getScattererDepth(),
                            this.getScattererDistDeg(),
                            this.DEBUG);
                    relPhases.addAll(calcRelPhaseList);
                } catch (ScatterArrivalFailException e) {
                    Alert.warning(e.getMessage(),
                            "    Skipping this relative phase");
                    if (verbose || DEBUG) {
                        e.printStackTrace();
                    }
                }

            }
            relativeArrival = SeismicPhase.getEarliestArrival(relPhases, degrees);
        }
        return relativeArrival;
    }

    @Override
    public void validateArguments() throws TauModelException {
        if (this.getTauModel() == null) {
            throw new TauModelException("Model for '"+this.getTauModelName()+"' is null, unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getSourceDepth()) {
            throw new TauModelException("Source depth of "+getSourceDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getReceiverDepth()) {
            throw new TauModelException("Receiver depth of "+getReceiverDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (this.getTauModel().getRadiusOfEarth() < getScattererDepth()) {
            throw new TauModelException("Scatterer depth of "+getScattererDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+this.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
    }

    public List<Arrival> calcTime(double degrees) throws TauModelException {
        List<Double> dList = Arrays.asList(new Double[] {degrees});
        return calcTime(dList);
    }

    public List<Arrival> calcTime(List<Double> degreesList) throws TauModelException {
        validateArguments();
        depthCorrect();
        clearArrivals();
        SeismicPhase phase;
        clearArrivals();
        List<SeismicPhase> phaseList = getSeismicPhases();
        for (double degrees : degreesList) {
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = phaseList.get(phaseNum);
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
                Arrival relativeArrival = calculateRelativeArrival(degrees);
                for (Arrival arrival : phaseArrivals) {
                    arrival.setRelativeToArrival(relativeArrival);
                }
                if (!onlyFirst) {
                    for (Arrival arrival : phaseArrivals) {
                        arrivals.add(arrival);
                    }
                } else {
                    if (phaseArrivals.size() > 0) {
                        arrivals.add(phaseArrivals.get(0));
                    }
                }
            }
        }
        return sortArrivals(arrivals);
    }


    public List<Arrival> calcTakeoff(double takeoffAngle) throws TauModelException {
        List<Double> dList = Arrays.asList(new Double[] {takeoffAngle});
        return calcTakeoff(dList);
    }

    public List<Arrival> calcTakeoff(List<Double> takeoffAngleList) throws TauModelException {
        stationLat = Double.MAX_VALUE;
        stationLon = Double.MAX_VALUE;
        depthCorrect();
        clearArrivals();
        SeismicPhase phase;
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<Arrival> arrivals = new ArrayList<>();
        for (double takeoffAngle : takeoffAngleList) {
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = phaseList.get(phaseNum);
                if (phase.getDownGoing()[0] == (takeoffAngle <= 90)) {
                    // check both downgoing or both upgoing
                    double rayParam = phase.calcRayParamForTakeoffAngle(takeoffAngle);
                    Arrival phaseArrival;
                    try {
                        if (phase.getMinRayParam() <= rayParam && rayParam <= phase.getMaxRayParam()) {
                            phaseArrival = phase.shootRay(rayParam);
                            arrivals.add(phaseArrival);
                        }
                    } catch (NoSuchLayerException e) {
                        Alert.warning("NoSuchLayerException", e.getMessage());
                    } catch (SlownessModelException e) {
                        Alert.warning("SlownessModelException", e.getMessage());
                    }
                }
            }
        }
        this.arrivals = sortArrivals(arrivals);
        return this.arrivals;    }

    public List<Arrival> calcRayParameter(double rayparameter) throws TauModelException {
        List<Double> dList = Arrays.asList(new Double[] {rayparameter});
        return calcRayParameter(dList);
    }

    /**
     * Shoots ray parameters for each phases from the source.
     * @param rayParameterList ray parameter list in s/deg
     * @throws TauModelException
     */
    public List<Arrival> calcRayParameterSDeg(List<Double> rayParameterList) throws TauModelException {
        List<Double> rayParameterListRad = new ArrayList<>();
        for (Double d : rayParameterList) {
            rayParameterListRad.add(d/SphericalCoords.dtor);
        }
        return calcRayParameter(rayParameterListRad);
    }

    /**
     * Shoots ray parameters for each phases from the source.
     * @param rayParameterList ray parameter list in s/radian
     * @throws TauModelException
     */
    public List<Arrival> calcRayParameter(List<Double> rayParameterList) throws TauModelException {
        stationLat = Double.MAX_VALUE;
        stationLon = Double.MAX_VALUE;
        depthCorrect();
        clearArrivals();
        SeismicPhase phase;
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<Arrival> arrivals = new ArrayList<>();
        for (Double rayParam : rayParameterList) {
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                phase = phaseList.get(phaseNum);
                Arrival phaseArrival;
                try {
                    if (phase.getMinRayParam() <= rayParam && rayParam <= phase.getMaxRayParam()) {
                        phaseArrival = phase.shootRay(rayParam);
                        arrivals.add(phaseArrival);
                    }
                } catch (NoSuchLayerException e) {
                    Alert.warning("NoSuchLayerException", e.getMessage());
                } catch (SlownessModelException e) {
                    Alert.warning("SlownessModelException", e.getMessage());
                }
            }
        }
        this.arrivals = sortArrivals(arrivals);
        return this.arrivals;
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
     * @deprecated use setSourceDepth instead
     */
    @Deprecated
    public void depthCorrect(double depth) throws TauModelException {
        depthCorrect(depth, getReceiverDepth());
    }

    /**
     *
     * In general, this is called by each tool's calculate methods, and so should
     * not need to be called by outside code. Most of the time calling setSourceDepth
     * and setReceiverDepth is preferred, allowing the tool to choose when to call depthCorrect.
     *
     * @param depth the source depth
     * @param receiverDepth the receiver depth
     * @throws TauModelException
     */
    public void depthCorrect(double depth, double receiverDepth) throws TauModelException {
        depthCorrect(depth, receiverDepth, getScattererDepth());
    }

    /**
     *
     * In general, this is called by each tool's calculate methods, and so should
     * not need to be called by outside code. Most of the time calling setSourceDepth
     * and setReceiverDepth and setScatterDepth is preferred, allowing the tool to choose when to call depthCorrect.
     *
     * @param depth the source depth
     * @param receiverDepth the receiver depth
     * @param scatterDepth scatterer depth, set to zero if no scattering
     * @throws TauModelException
     */
    public void depthCorrect(double depth, double receiverDepth, double scatterDepth) throws TauModelException {
        if(tModDepth == null || tModDepth.getSourceDepth() != depth) {
            setReceiverDepth(receiverDepth);
            tModDepth = tMod.depthCorrect(depth);
            tModDepth = tModDepth.splitBranch(receiverDepth);
            clearPhases();
        }
        if (receiverDepth != getReceiverDepth()) {
            setReceiverDepth(receiverDepth);
            tModDepth = tModDepth.splitBranch(receiverDepth); // if already split on receiver depth this does nothing
            clearPhases();
        }
        if (scatterDepth != getScattererDepth()) {
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
        for(int phaseNameNum = 0; phaseNameNum < phaseNames.size(); phaseNameNum++) {
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
            if(!alreadyAdded) {
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
                    Alert.warning(e.getMessage()+", skipping this phase");
                    if (verbose || DEBUG) {
                        e.printStackTrace();
                    }
                } catch(TauModelException e) {
                    Alert.warning("Error with phase=" + tempPhaseName,
                                  e.getMessage() + "\nSkipping this phase");
                    if (verbose || DEBUG) {
                        e.printStackTrace();
                    }
                } finally {
                    if(verbose) {
                        Alert.info("-----------------");
                    }
                }
            }
        }
        phases = newPhases;
    }

    public void printResult(PrintWriter out) throws IOException {
        if (outputFormat.equals(TauP_Tool.JSON)) {
            printResultJSON(out);
        } else {
            printResultText(out);
        }
    }

    public void printResultText(PrintWriter out) throws IOException {
        Arrival currArrival;
        int maxNameLength = 5;
        int maxPuristNameLength = 5;
        for(int j = 0; j < arrivals.size(); j++) {
            if(((Arrival)arrivals.get(j)).getName().length() > maxNameLength) {
                maxNameLength = ((Arrival)arrivals.get(j)).getName()
                        .length();
            }
            if(((Arrival)arrivals.get(j)).getPuristName().length() > maxPuristNameLength) {
                maxPuristNameLength = ((Arrival)arrivals.get(j)).getPuristName()
                        .length();
            }
        }
        Format phaseFormat = new Format("%-" + maxNameLength + "s");
        Format phasePuristFormat = new Format("%-" + maxPuristNameLength + "s");
        if(!(onlyPrintRayP || onlyPrintTime)) {
            String modelLine =  "\nModel: " + modelName;
            if (getReceiverDepth() != 0.0) {
                modelLine += "  Receiver Depth: "+getReceiverDepth()+" km";
            }
            if (getScattererDistDeg() != 0.0) {
                modelLine += "  Scatter Depth: "+ getScattererDepth()+" km Dist: "+ getScattererDistDeg() ;
            }
            out.println(modelLine);
            String lineOne = "Distance   Depth   " + phaseFormat.form("Phase")
                    + "   Travel    Ray Param  Takeoff  Incident  Purist   "+phasePuristFormat.form("Purist");
            String lineTwo = "  (deg)     (km)   " + phaseFormat.form("Name ")
                    + "   Time (s)  p (s/deg)   (deg)    (deg)   Distance   "+phasePuristFormat.form("Name");
            if (relativePhaseName != "") {
                lineOne += " Relative to";
                for (int s=0; s<(11-relativePhaseName.length())/2;s++) {
                    lineTwo += " ";
                }
                lineTwo += "  "+phaseFormat.form(relativePhaseName);
            }
            out.println(lineOne);
            out.println(lineTwo);
            for(int i = 0; i < Math.max(lineOne.length(), lineTwo.length()); i++) {
                out.write("-");
            }
            out.write("\n");
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.get(j);
                out.print(Outputs.formatDistance(currArrival.getSearchDistDeg()));
                out.print(Outputs.formatDepth(depth) + "   ");
                out.print(phaseFormat.form(currArrival.getName()));
                out.print("  "
                        + Outputs.formatTime(currArrival.getTime())
                        + "  "
                        + Outputs.formatRayParam(currArrival.getRayParam() / Arrival.RtoD) + "  ");
                out.print(Outputs.formatDistance(currArrival.getTakeoffAngle())+" ");
                out.print(Outputs.formatDistance(currArrival.getIncidentAngle())+" ");
                out.print(Outputs.formatDistance(currArrival.getDistDeg()));
                if(currArrival.getName().equals(currArrival.getPuristName())) {
                    out.print("   = ");
                } else {
                    out.print("   * ");
                }
                out.print(phasePuristFormat.form(currArrival.getPuristName()));
                if (relativePhaseName != "") {
                    if (currArrival.isRelativeToArrival()) {
                        out.print(" "+Outputs.formatTime(currArrival.getTime() - currArrival.getRelativeToArrival().getTime()));
                        out.print(" +"+phaseFormat.form(currArrival.getRelativeToArrival().getName()));
                    } else {
                        out.print(phaseFormat.form("no arrival"));
                    }
                }
                try {
                    double ampFactor = currArrival.getAmplitudeFactor();
                    out.print(" " + Outputs.formatAmpFactor(ampFactor));
                } catch (NoSuchMatPropException e) {
                    e.printStackTrace();
                } catch (NoSuchLayerException e) {
                    e.printStackTrace();
                } catch (SlownessModelException e) {
                    e.printStackTrace();
                } catch (TauModelException | VelocityModelException e) {
                    e.printStackTrace();
                }

                out.println();
            }
        } else if(onlyPrintTime) {
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.get(j);
                out.print(String.valueOf((float)(currArrival.getTime())) + " ");
            }
            out.println();
        } else if(onlyPrintRayP) {
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.get(j);
                out.write(String.valueOf((float)(Math.PI / 180.0 * currArrival.getRayParam()))
                        + " ");
            }
            out.println();
        }
        out.println();
        out.flush();
    }

    public void printResultJSON(PrintWriter out) {
        String s = resultAsJSON(modelName, depth, getReceiverDepth(), getPhaseNames(), arrivals);
        out.println(s);
    }

    public static String resultAsJSON(String modelName,
                                      double depth,
                                      double receiverDepth,
                                      String[] phases,
                                      List<Arrival> arrivals) {
        return resultAsJSON(modelName, depth, receiverDepth, phases, arrivals, false, false);
    }

    public static String resultAsJSON(String modelName,
                                      double depth,
                                      double receiverDepth,
                                      String[] phases,
                                      List<Arrival> arrivals,
                                      boolean withPierce,
                                      boolean withPath) {
        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SS = S+S;
        String SQ = S+Q;
        String SSQ = S+SQ;
        String SSSQ = S+SSQ;
        // use cast to float to limit digits printed
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("{");
        out.println(SQ+"model"+QCQ+modelName+QCOMMA);
        out.println(SQ+"sourcedepth"+QC+(float)depth+COMMA);
        out.println(SQ+"receiverdepth"+QC+(float)receiverDepth+COMMA);
        out.print(SQ+"phases"+Q+": [");
        for(int p=0; p<phases.length; p++) {
            out.print(" "+Q+phases[p]+Q);
            if ( p != phases.length-1) {
                out.print(COMMA);
            }
        }
        out.println(" ]"+COMMA);
        out.println(SQ+"arrivals"+Q+": [");
        for(int j = 0; j < arrivals.size(); j++) {
            Arrival currArrival = (Arrival)arrivals.get(j);
            out.print(currArrival.asJSON(true, SS, withPierce, withPath));
            if (j != arrivals.size()-1) {
                out.print(COMMA);
            }
            out.println();
        }
        out.println(S+"]");
        out.print("}");
        return sw.toString();
    }

    /**
     * preforms intialization of the tool. Properties are queried for the the
     * default model to load, source depth to use, phases to use, etc. Note that
     * because of the IO inherent in these operations, this method is not
     * appropriate for Applets. Applets should load TauModels themselves and use
     * the setTauModel(TauModel) method.
     */
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
        // check for command line arg distance in km
        for (double distKilometers : distKilometersList) {
                degreesList.add(distKilometers / getTauModel().getRadiusOfEarth()
                        * 180.0 / Math.PI);
        }
    }

    public void printHelp() {
        Alert.info("Enter:\nh for new depth\nr to recalculate\n"
                + "p to append phases, \nc to clear phases\n"
                + "l to list phases\n"
                + "s for new station lat lon\ne for new event lat lon\n"
                + "a for new azimuth\nb for new back azimuth\n"
                + "t for takeoff angle\n"
                + "m for new model or \nq to quit.\n");
    }

    public void start() throws IOException, TauModelException, TauPException {
        if((degreesList.size() != 0 || takeoffAngle != Double.MAX_VALUE
                || shootRayp != Double.MAX_VALUE
                || (stationLat != Double.MAX_VALUE
                && stationLon != Double.MAX_VALUE
                && eventLat != Double.MAX_VALUE && eventLon != Double.MAX_VALUE))) {
            /* enough info given on cmd line, so just do one calc. */
            if (takeoffAngle != Double.MAX_VALUE) {
                calcTakeoff(takeoffAngle);
            } else if (shootRayp != Double.MAX_VALUE) {
                calcRayParameter(shootRayp/SphericalCoords.dtor);
            } else {
                if(degreesList.size() == 0) {
                    Double[] evlatlon = new Double[] {eventLat, eventLon};
                    List<Double[]> stalatlonList = new ArrayList<>();
                    stalatlonList.add(new Double[] {stationLat, stationLon});
                    calcEventStation(evlatlon, stalatlonList);
                } else {
                    calculate(degreesList);
                }
            }
            printResult(getWriter());
        } else {
            /* interactive mode... */
            long prevTime = 0;
            long currTime;
            char readMode = 'd';
            double tempDepth = depth;
            setSourceDepth(depth);
            StreamTokenizer tokenIn = new StreamTokenizer(new InputStreamReader(System.in));
            tokenIn.parseNumbers();
            tokenIn.wordChars(',', ',');
            tokenIn.wordChars('_', '_');
            tokenIn.wordChars('^', '^');
            tokenIn.ordinaryChar('/');
            tokenIn.wordChars('/', '/');
            tokenIn.commentChar('#');
            printHelp();
            do {
                switch(readMode){
                    case 'h':
                        // new source depth
                        System.out.print("Enter Depth: ");
                        tokenIn.nextToken();
                        tempDepth = tokenIn.nval;
                        if(tempDepth < 0.0
                                || tempDepth > tMod.getRadiusOfEarth()) {
                            Alert.warning("Depth must be >= 0.0 and <= tMod.getRadiusOfEarth().",
                                          "depth = " + tempDepth
                                                  + " getRadiusOfEarth= "
                                                  + tMod.getRadiusOfEarth());
                            continue;
                        }
                        prevTime = System.currentTimeMillis();
                        setSourceDepth(tempDepth);
                        currTime = System.currentTimeMillis();
                        if(verbose) {
                            Alert.info("depthCorrect time="
                                    + (currTime - prevTime));
                        }
                        readMode = 'd';
                        break;
                    case 'd':
                        // new distance or option
                        System.out.print("Enter Distance or Option [hrpclseabmqt]: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            double degrees = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("degrees=" + degrees);
                            }
                            degreesList.clear();
                            degreesList.add(degrees);
                            calculate(degreesList);
                            printResult(getWriter());
                        } else {
                            if(tokenIn.ttype == StreamTokenizer.TT_EOF
                                    || (tokenIn.ttype == StreamTokenizer.TT_WORD && (tokenIn.sval.equalsIgnoreCase("q")
                                            || tokenIn.sval.equalsIgnoreCase("quit")
                                            || tokenIn.sval.equalsIgnoreCase("exit") || tokenIn.sval.equalsIgnoreCase("bye")))) {
                                readMode = 'q';
                            } else if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                                if(tokenIn.sval.equalsIgnoreCase("l")) {
                                    readMode = 'l';
                                } else if(tokenIn.sval.equalsIgnoreCase("c")) {
                                    readMode = 'c';
                                } else if(tokenIn.sval.equalsIgnoreCase("s")) {
                                    readMode = 's';
                                } else if(tokenIn.sval.equalsIgnoreCase("e")) {
                                    readMode = 'e';
                                } else if(tokenIn.sval.equalsIgnoreCase("a")) {
                                    readMode = 'a';
                                } else if(tokenIn.sval.equalsIgnoreCase("b")) {
                                    readMode = 'b';
                                } else if(tokenIn.sval.equalsIgnoreCase("r")) {
                                    readMode = 'r';
                                } else if(tokenIn.sval.equalsIgnoreCase("p")) {
                                    readMode = 'p';
                                } else if(tokenIn.sval.equalsIgnoreCase("m")) {
                                    readMode = 'm';
                                } else if(tokenIn.sval.equalsIgnoreCase("h")) {
                                    readMode = 'h';
                                } else if(tokenIn.sval.equalsIgnoreCase("x")) {
                                    readMode = 'x';
                                } else if(tokenIn.sval.equalsIgnoreCase("t")) {
                                    readMode = 't';
                                } else if(tokenIn.sval.equalsIgnoreCase("?")) {
                                    printHelp();
                                } else {
                                    Alert.warning("I don't understand this option",
                                            tokenIn.sval);
                                    printHelp();
                                }
                            } else {
                                printHelp();
                            }
                        }
                        break;
                    case 'r':
                        // recalulate
                        if(degreesList.size() != 0) {
                            calculate(degreesList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'p':
                        // append phases
                        System.out.print("Enter phases (ie P,p,PcP,S): ");
                        tokenIn.ordinaryChars('0', '9');
                        tokenIn.ordinaryChar('.');
                        tokenIn.ordinaryChar('-');
                        tokenIn.wordChars('0', '9');
                        tokenIn.wordChars('.', '.');
                        tokenIn.wordChars('-', '-');
                        tokenIn.ordinaryChar(' ');
                        tokenIn.wordChars(' ', ' ');
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                            parsePhaseList(tokenIn.sval);
                            recalcPhases();
                        } else {
                            Alert.warning("Input phases not recognized.",
                                          "Please retry.");
                        }
                        tokenIn.parseNumbers();
                        tokenIn.ordinaryChar(' ');
                        tokenIn.whitespaceChars(' ', ' ');
                        readMode = 'd';
                        break;
                    case 'l':
                        // list phases
                        int numPhases = phaseNames.size();
                        String output = numPhases + " phases.";
                        Alert.info(output);
                        output = "";
                        for(int i = 0; i < numPhases; i++) {
                            output += phaseNames.get(i).getName();
                            if(i < numPhases - 1) {
                                output += ",";
                            }
                        }
                        Alert.info(output);
                        readMode = 'd';
                        break;
                    case 'c':
                        // clear phases and then enter new phases
                        clearPhaseNames();
                        readMode = 'p';
                        break;
                    case 'a':
                        // event to station azimuth
                        System.out.print("Enter azimuth: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            azimuth = tokenIn.nval;
                            stationLat = Double.MAX_VALUE;
                            stationLon = Double.MAX_VALUE;
                            if(DEBUG) {
                                Alert.info("azimuth=" + azimuth);
                            }
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead.");
                            printHelp();
                            break;
                        }
                        if(eventLat == Double.MAX_VALUE
                                || eventLon == Double.MAX_VALUE) {
                            readMode = 'e';
                        } else if(degreesList.size() == 0) {
                            readMode = 'd';
                        } else {
                            calculate(degreesList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'b':
                        // event to station back azimuth (ie station to event
                        // azimuth)
                        System.out.print("Enter back azimuth: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            backAzimuth = tokenIn.nval;
                            setEventLatLon( Double.MAX_VALUE, Double.MAX_VALUE);
                            if(DEBUG) {
                                Alert.info("backAzimuth=" + backAzimuth);
                            }
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead");
                            printHelp();
                            break;
                        }
                        if(stationLat == Double.MAX_VALUE
                                || stationLon == Double.MAX_VALUE) {
                            readMode = 's';
                        } else if(degreesList.size() == 0) {
                            readMode = 'd';
                        } else {
                            calculate(degreesList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'e':
                        // event lat and lon
                        System.out.print("Enter event lat and lon: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            double evLat = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("eventLat=" + eventLat);
                            }
                            tokenIn.nextToken();
                            if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                                double evLon = tokenIn.nval;
                                setEventLatLon(evLat, evLon);
                                if(DEBUG) {
                                    Alert.info("eventLon=" + eventLon);
                                }
                            } else {
                                printHelp();
                            }
                        } else {
                            printHelp();
                        }
                        if(stationLat != Double.MAX_VALUE
                                && stationLon != Double.MAX_VALUE) {

                            Double[] evlatlon = new Double[] {eventLat, eventLon};
                            List<Double[]> stalatlonList = new ArrayList<>();
                            stalatlonList.add(new Double[] {stationLat, stationLon});
                            calcEventStation(evlatlon, stalatlonList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 's':
                        // station lat and lon
                        System.out.print("Enter station lat and lon: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            stationLat = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("stationLat=" + stationLat);
                            }
                            tokenIn.nextToken();
                            if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                                stationLon = tokenIn.nval;
                                if(DEBUG) {
                                    Alert.info("stationLon=" + stationLon);
                                }
                            } else {
                                printHelp();
                                break;
                            }
                        } else {
                            printHelp();
                            break;
                        }
                        if(eventLat != Double.MAX_VALUE
                                && eventLon != Double.MAX_VALUE) {
                            Double[] evlatlon = new Double[] {eventLat, eventLon};
                            List<Double[]> stalatlonList = new ArrayList<>();
                            stalatlonList.add(new Double[] {stationLat, stationLon});
                            calcEventStation(evlatlon, stalatlonList);
                            printResult(getWriter());
                        }
                        readMode = 'd';
                        break;
                    case 'm':
                        // change model
                        tokenIn.ordinaryChars('0', '9');
                        tokenIn.wordChars('0', '9');
                        tokenIn.ordinaryChars('.', '.');
                        tokenIn.wordChars('.', '.');
                        tokenIn.ordinaryChars('-', '-');
                        tokenIn.wordChars('-', '-');
                        String oldModelName = modelName;
                        TauModel oldTMod = tMod;
                        TauModel oldTModDepth = tModDepth;
                        System.out.print("Enter model name: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                            modelName = tokenIn.sval;
                        }
                        tokenIn.ordinaryChars('0', '9');
                        tokenIn.ordinaryChars('.', '.');
                        tokenIn.ordinaryChars('-', '-');
                        tokenIn.parseNumbers();
                        if(!modelName.equals(oldModelName)) {
                            try {
                                readTauModel();
                                setSourceDepth(depth);
                            } catch(TauModelException e) {
                                if (e.getCause() instanceof InvalidClassException) {
                                    Alert.warning("Model file "
                                                  + modelName
                                                  + " is not compatible with the current version.",
                                          "Recreate using taup_create. Still using model "
                                                  + oldModelName + ".");
                                } else {
                                    Alert.warning("I can't load model file "
                                                  + modelName, "Still using model "
                                                  + oldModelName + ".");
                                }
                                modelName = oldModelName;
                                tMod = oldTMod;
                                tModDepth = oldTModDepth;
                            }
                        }
                        readMode = 'd';
                        break;
                    case 't':
                        System.out.print("Enter takeoff angle (deg): ");
                        // takeoff angle
                        double takeoffAngle;
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            takeoffAngle = tokenIn.nval;
                            calcTakeoff(takeoffAngle);
                            printResult(getWriter());
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead.");
                            printHelp();
                            break;
                        }
                        readMode = 'd';
                        break;
                    case 'z':
                        System.out.print("Enter ray parameter (s/deg): ");
                        // takeoff angle
                        double shootRayp;
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            shootRayp = tokenIn.nval;
                            calcRayParameter(shootRayp/SphericalCoords.dtor);
                            printResult(getWriter());
                        } else {
                            Alert.warning("Expected a number.", "got "
                                    + tokenIn + " instead.");
                            printHelp();
                            break;
                        }
                        readMode = 'd';
                        break;
                    case 'q':
                        return;
                }
            } while(tokenIn.ttype == StreamTokenizer.TT_NUMBER
                    || tokenIn.ttype != StreamTokenizer.TT_WORD
                    || (tokenIn.ttype == StreamTokenizer.TT_WORD && !tokenIn.sval.equalsIgnoreCase("q")));
        }
    }

    public void destroy() throws TauPException {
        if(writer != null) {
            writer.close();
            writer = null;
        }
    }

    public void printStdUsageHead() {
        TauP_Tool.printStdUsageHead(this.getClass());
    }

    /** Prints the command line arguments common to all TauP tools. */
    public void printStdUsage() {
        printStdUsageHead();
        printPhaseUsage();
        printDistanceUsage();
    }

    public void printPhaseUsage() {
        Alert.info("-ph phase list     -- comma separated phase list\n"
                + "-pf phasefile      -- file containing phases\n");
        printModDepthUsage();
    }

    public void printDistanceUsage() {
        Alert.info("Distance is given by:\n\n"
                + "-deg degrees       -- distance in degrees,\n"
                + "-km kilometers     -- distance in kilometers,\n"
                + "                      assumes radius of earth is 6371km,\n\n"
                + "or by giving the station and event latitude and lonitude,\n"
                + "                      assumes a spherical earth,\n\n"
                + "-sta[tion] lat lon -- sets the station latitude and longitude\n"
                + "-evt       lat lon -- sets the event latitude and longitude\n\n"
                + "or by giving the takeoff angle,\n"
                + "--takeoff angle    -- takeoff angle from the source\n"
                + "                      zero is down, 90 horizontal, 180 is up\n\n"
                + "or by giving the ray parameter,\n"
                + "--shootray param   -- ray parameter from the source in s/deg\n"
                + "                      up or down is determined by the phase\n\n"

        );
    }

    public void printLimitUsage() {
        Alert.info("--first            -- only output the first arrival for each phase, no triplications\n"
                    + "--rayp             -- only output the ray parameter\n"
                    + "--time             -- only output travel time\n"
                    + "--rel phasename    -- also output relative travel time\n\n"
                    + "--json             -- output travel times as json\n"
        );
    }

    public void printUsage() {
        printStdUsage();
        printLimitUsage();
        printStdUsageTail();
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
        if(x == xa) {
            return ya;
        }
        if(x == xb) {
            return yb;
        }
        return (yb - ya) * (x - xa) / (xb - xa) + ya;
    }

    /**
     * Allows TauP_Time to run as an application. Creates an instance of
     * TauP_Time.
     *
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.TIME, args);
    }

}
