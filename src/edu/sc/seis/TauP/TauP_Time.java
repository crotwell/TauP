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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import org.apache.log4j.BasicConfigurator;

/**
 * Calculate travel times for different branches using linear interpolation
 * between known slowness samples.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
public class TauP_Time {

    /** Turns on debugging output. */
    public boolean DEBUG = false;

    /** Turns on verbose output. */
    public boolean verbose = false;

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
    protected Vector phases = new Vector(10);

    /** names of phases to be used, ie PKIKP. */
    protected Vector phaseNames = new Vector(10);

    protected double depth = 0.0;

    protected double degrees = Double.MAX_VALUE;

    protected double azimuth = Double.MAX_VALUE;

    protected double backAzimuth = Double.MAX_VALUE;

    protected double stationLat = Double.MAX_VALUE;

    protected double stationLon = Double.MAX_VALUE;

    protected double eventLat = Double.MAX_VALUE;

    protected double eventLon = Double.MAX_VALUE;

    protected Vector arrivals = new Vector(10);

    protected boolean GUI = false;

    protected boolean onlyPrintRayP = false;

    protected boolean onlyPrintTime = false;

    protected String outFile = "";

    protected DataOutputStream dos;

    protected Properties toolProps;

    protected Outputs outForms;

    /* Constructors */
    public TauP_Time() {
        try {
            toolProps = PropertyLoader.load();
        } catch(Exception e) {
            Alert.warning("Unable to load properties, using defaults.",
                          e.getMessage());
            toolProps = new Properties();
        }
        outForms = new Outputs(toolProps);
    }

    public TauP_Time(TauModel tMod) throws TauModelException {
        this();
        this.tMod = tMod;
        this.tModDepth = tMod;
        modelName = tMod.sMod.vMod.getModelName();
    }

    /**
     * creates a TauP_Time object with the tau model specified by modelName
     * already loaded.
     * 
     * @throws TauModelException
     *             if the file can't be found or is corrupted in some way.
     */
    public TauP_Time(String modelName) throws TauModelException {
        this();
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

    /* Get/Set methods */
    public String[] getPhaseNames() {
        String[] phases = new String[phaseNames.size()];
        for(int i = 0; i < phaseNames.size(); i++) {
            phases[i] = ((PhaseName)phaseNames.elementAt(i)).getName();
        }
        return phases;
    }

    public String getPhaseNameString() {
        // in case of empty phase list
        if(getNumPhases() == 0) return "";
        String phases = ((PhaseName)phaseNames.elementAt(0)).getName();
        for(int i = 1; i < getNumPhases(); i++) {
            phases += "," + ((PhaseName)phaseNames.elementAt(i)).getName();
        }
        return phases;
    }

    public void setPhaseNames(String[] phaseNames) {
        this.phaseNames.removeAllElements();
        for(int i = 0; i < phaseNames.length; i++) {
            appendPhaseName(phaseNames[i]);
        }
    }

    public void setPhaseNames(PhaseName[] phaseNames) {
        this.phaseNames.removeAllElements();
        for(int i = 0; i < phaseNames.length; i++) {
            this.phaseNames.addElement(phaseNames[i]);
        }
    }

    public static List getPhaseNames(String phaseName) {
        List names = new ArrayList();
        if(phaseName.equalsIgnoreCase("ttp")
                || phaseName.equalsIgnoreCase("tts")
                || phaseName.equalsIgnoreCase("ttbasic")
                || phaseName.equalsIgnoreCase("tts+")
                || phaseName.equalsIgnoreCase("ttp+")
                || phaseName.equalsIgnoreCase("ttall")) {
            if(phaseName.equalsIgnoreCase("ttp")
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
            if(phaseName.equalsIgnoreCase("tts")
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
            if(phaseName.equalsIgnoreCase("ttp+")
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
            if(phaseName.equalsIgnoreCase("tts+")
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
            if(phaseName.equalsIgnoreCase("ttbasic")
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
            if(phaseName.equalsIgnoreCase("ttall")) {
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
        return names;
    }

    public synchronized void appendPhaseName(String phaseName) {
        List names = getPhaseNames(phaseName);
        Iterator it = names.iterator();
        while(it.hasNext()) {
            appendPhaseName(new PhaseName((String)it.next()));
        }
    }

    public synchronized void appendPhaseName(PhaseName phaseName) {
        boolean unique = true;
        if(phaseName.name == null || phaseName.name.length() == 0) {
            // make sure not null string
            return;
        }
        for(int i = 0; i < phaseNames.size(); i++) {
            if(((PhaseName)phaseNames.elementAt(i)).equals(phaseName)) {
                unique = false;
                return;
            }
        }
        if(unique) {
            this.phaseNames.addElement(phaseName);
        }
    }

    public int getNumPhases() {
        return phaseNames.size();
    }

    public void clearPhaseNames() {
        phases.removeAllElements();
        phaseNames.removeAllElements();
    }

    public double getSourceDepth() {
        return Double.valueOf(toolProps.getProperty("taup.source.depth", "0.0"))
                .doubleValue();
    }

    public void setSourceDepth(double depth) {
        this.depth = depth;
        toolProps.put("taup.source.depth", Double.toString(depth));
    }

    public String getTauModelName() {
        return modelName;
    }

    public TauModel getTauModel() {
        return tMod;
    }

    public void setTauModel(TauModel tMod) {
        this.tMod = tMod;
        this.tModDepth = tMod;
        modelName = tMod.sMod.vMod.getModelName();
        toolProps.put("taup.model.name", modelName);
    }

    public void loadTauModel(String modelName) throws FileNotFoundException,
            InvalidClassException, IOException, StreamCorruptedException,
            OptionalDataException, TauModelException {
        this.modelName = modelName;
        readTauModel();
        this.modelName = tMod.sMod.vMod.getModelName();
    }

    public double[] getDisconDepths() {
        return tMod.sMod.vMod.getDisconDepths();
    }

    public void clearArrivals() {
        arrivals.removeAllElements();
    }

    public int getNumArrivals() {
        return arrivals.size();
    }

    public Arrival getArrival(int i) {
        return (Arrival)((Arrival)arrivals.elementAt(i)).clone();
    }

    public Arrival[] getArrivals() {
        Arrival[] returnArrivals = new Arrival[arrivals.size()];
        for(int i = 0; i < arrivals.size(); i++) {
            returnArrivals[i] = (Arrival)((Arrival)arrivals.elementAt(i)).clone();
        }
        return returnArrivals;
    }

    /* Normal methods */
    /**
     * Reads the velocity model, slowness model, and tau model from a file saved
     * using Java's Serializable interface. Performs a depth correction if the
     * current depth is not 0.0
     */
    protected void readTauModel() throws FileNotFoundException,
            InvalidClassException, IOException, StreamCorruptedException,
            OptionalDataException, TauModelException {
        try {
            TauModel tModLoad = TauModelLoader.load(modelName,
                                                    toolProps.getProperty("taup.model.path"));
            if(tModLoad != null) {
                tMod = tModLoad;
                tModDepth = tMod;
                this.modelName = tMod.sMod.vMod.getModelName();
            }
        } catch(ClassNotFoundException e) {
            Alert.error("Caught ClassNotFoundException",
                        e.getMessage()
                                + "\nThere must be something wrong with your installation of TauP.");
            throw new RuntimeException("Caught ClassNotFoundException"
                                               + e.getMessage()
                                               + "\nThere must be something wrong with your installation of TauP.",
                                       e);
        } catch(InvalidClassException e) {
            Alert.error("Model file "
                                + modelName
                                + " is not compatible with the current version.",
                        "Recreate using taup_create.");
            throw new RuntimeException("Model file " + modelName
                    + " is not compatible with the current version."
                    + "Recreate using taup_create.", e);
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
     * T2, SS in T3, and S^410S in T5.
     */
    public void parsePhaseList(String phaseList) {
        int offset = 0;
        int phaseSepIndex;
        String phaseEntry;
        phaseList = phaseList.replace(' ', ',');
        // remove any empty phases, ie two commas next to each other
        // should be replaced with one comma
        phaseSepIndex = phaseList.indexOf(",,", offset);
        while(phaseSepIndex != -1) {
            phaseList = phaseList.substring(0, phaseSepIndex)
                    + phaseList.substring(phaseSepIndex + 1);
            phaseSepIndex = phaseList.indexOf(",,", offset);
        }
        // remove comma at begining
        if(phaseList.charAt(0) == ',') {
            if(phaseList.length() > 1) {
                phaseList = phaseList.substring(1);
            } else {
                // phaseList is just a single comma, no phases, so just return
                return;
            }
        }
        // and comma at end
        if(phaseList.charAt(phaseList.length() - 1) == ',') {
            // we know that the length is > 1 as if not then we would have
            // returned from the previous if
            phaseList = phaseList.substring(0, phaseList.length() - 1);
        }
        while(offset < phaseList.length()) {
            phaseSepIndex = phaseList.indexOf(',', offset);
            if(phaseSepIndex != -1) {
                phaseEntry = phaseList.substring(offset, phaseSepIndex);
                offset = phaseSepIndex + 1;
            } else {
                phaseEntry = phaseList.substring(offset);
                offset = phaseList.length();
            }
            phaseSepIndex = phaseEntry.indexOf('-');
            if(phaseSepIndex == -1) {
                /* no optional dash argument, so just add the name. */
                appendPhaseName(phaseEntry);
            } else {
                if(phaseSepIndex == phaseEntry.length() - 2
                        && Character.isDigit(phaseEntry.charAt(phaseEntry.length() - 1))) {
                    /*
                     * There is an optional argument, so store it and the phase
                     * name.
                     */
                    appendPhaseName(new PhaseName(phaseEntry.substring(0,
                                                                       phaseSepIndex),
                                                  Integer.valueOf(phaseEntry.substring(phaseSepIndex + 1,
                                                                                       phaseEntry.length()))
                                                          .intValue()));
                } else {
                    Alert.warning("Problem with phase=" + phaseEntry,
                                  "Skipping this phase.");
                }
            }
        }
    }

    /**
     * Parses a comma separated list of distances and returns them in an array.
     */
    public double[] parseDegreeList(String degList) {
        int offset = 0;
        int commaIndex;
        String degEntry;
        int numDegrees = 0;
        double[] degreesFound = new double[degList.length()];
        while(offset < degList.length()) {
            commaIndex = degList.indexOf(',', offset);
            if(commaIndex != -1) {
                degEntry = degList.substring(offset, commaIndex);
                degreesFound[numDegrees] = Double.valueOf(degEntry)
                        .doubleValue();
                offset = commaIndex + 1;
                numDegrees++;
            } else {
                degEntry = degList.substring(offset);
                degreesFound[numDegrees] = Double.valueOf(degEntry)
                        .doubleValue();
                offset = degList.length();
                numDegrees++;
            }
        }
        double[] temp = new double[numDegrees];
        System.arraycopy(degreesFound, 0, temp, 0, numDegrees);
        degreesFound = temp;
        return degreesFound;
    }

    /*
     * parses the standard command line args for the taup package. Other tools
     * that subclass this class will likely override this.
     */
    protected String[] parseCmdLineArgs(String[] args) throws IOException {
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        boolean cmdLineArgPhase = false;
        boolean cmdLineArgPhaseFile = false;
        while(i < args.length) {
            if(args[i].equalsIgnoreCase("-help")) {
                printUsage();
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            } else if(args[i].equalsIgnoreCase("-version")) {
                Alert.info(Version.getVersion());
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            } else if(args[i].equalsIgnoreCase("-verbose")) {
                verbose = true;
            } else if(args[i].equalsIgnoreCase("-debug")) {
                verbose = true;
                DEBUG = true;
            } else if(args[i].equalsIgnoreCase("-gui")) {
                GUI = true;
            } else if(args[i].equalsIgnoreCase("-rayp")) {
                onlyPrintRayP = true;
                onlyPrintTime = false;
            } else if(args[i].equalsIgnoreCase("-time")) {
                onlyPrintTime = true;
                onlyPrintRayP = false;
            } else if(i < args.length - 1) {
                if(args[i].equalsIgnoreCase("-mod")
                        || args[i].equalsIgnoreCase("-model")) {
                    toolProps.put("taup.model.name", args[i + 1]);
                    i++;
                } else if(args[i].equalsIgnoreCase("-h")) {
                    toolProps.put("taup.source.depth", args[i + 1]);
                    i++;
                } else if(args[i].equalsIgnoreCase("-deg")) {
                    degrees = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(args[i].equalsIgnoreCase("-km")) {
                    degrees = Double.valueOf(args[i + 1]).doubleValue() / 6371
                            * 180.0 / Math.PI;
                    i++;
                } else if(args[i].equalsIgnoreCase("-az")) {
                    azimuth = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(args[i].equalsIgnoreCase("-baz")) {
                    backAzimuth = Double.valueOf(args[i + 1]).doubleValue();
                    i++;
                } else if(args[i].equalsIgnoreCase("-o")) {
                    outFile = args[i + 1];
                    i++;
                } else if(args[i].equalsIgnoreCase("-ph")) {
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
                } else if(args[i].equalsIgnoreCase("-pf")) {
                    cmdLineArgPhaseFile = true;
                    toolProps.put("taup.phase.file", args[i + 1]);
                    i++;
                } else if(i < args.length - 2) {
                    if(args[i].equalsIgnoreCase("-sta")
                            || args[i].equalsIgnoreCase("-station")) {
                        stationLat = Double.valueOf(args[i + 1]).doubleValue();
                        stationLon = Double.valueOf(args[i + 2]).doubleValue();
                        i += 2;
                    } else if(args[i].equalsIgnoreCase("-evt")
                            || args[i].equalsIgnoreCase("-event")) {
                        eventLat = Double.valueOf(args[i + 1]).doubleValue();
                        eventLon = Double.valueOf(args[i + 2]).doubleValue();
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

    public synchronized void sortArrivals() {
        if(arrivals.size() < 2) { return; }
        boolean sorted = false;
        int i, k = 0;
        Arrival currArrival, prevArrival;
        while(!sorted) {
            sorted = true;
            currArrival = (Arrival)arrivals.elementAt(0);
            for(int j = 0; j < arrivals.size() - 1; j++) {
                prevArrival = currArrival;
                currArrival = (Arrival)arrivals.elementAt(j + 1);
                if(prevArrival.time > currArrival.time) {
                    sorted = false;
                    arrivals.setElementAt(currArrival, j);
                    arrivals.setElementAt(prevArrival, j + 1);
                    currArrival = prevArrival;
                }
            }
        }
    }

    public void calculate(double degrees) throws TauModelException {
        depthCorrect(getSourceDepth());
        recalcPhases();
        calcTime(degrees);
    }

    public void calcTime(double degrees) {
        this.degrees = degrees;
        SeismicPhase phase;
        Arrival[] phaseArrivals;
        arrivals.removeAllElements();
        for(int phaseNum = 0; phaseNum < phases.size(); phaseNum++) {
            phase = (SeismicPhase)phases.elementAt(phaseNum);
            phase.setDEBUG(DEBUG);
            phase.calcTime(degrees);
            phaseArrivals = phase.getArrivals();
            for(int i = 0; i < phaseArrivals.length; i++) {
                arrivals.addElement(phaseArrivals[i]);
            }
        }
        sortArrivals();
    }

    /**
     * corrects the TauModel for the given source depth. It only performs the
     * correction of the model is not already corrected to that depth.
     */
    public void depthCorrect(double depth) throws TauModelException {
        if(tModDepth == null || tModDepth.getSourceDepth() != depth) {
            tModDepth = tMod.depthCorrect(depth);
            clearArrivals();
            recalcPhases();
        }
        setSourceDepth(depth);
    }

    /**
     * reclaulates the given phases using a possibly new or changed tau model.
     * This should not need to be called by outside classes as it is called by
     * depthCorrect, and calculate.
     */
    public synchronized void recalcPhases() {
        SeismicPhase seismicPhase;
        Vector newPhases = new Vector(phases.size());
        boolean alreadyAdded;
        String tempPhaseName;
        for(int phaseNameNum = 0; phaseNameNum < phaseNames.size(); phaseNameNum++) {
            tempPhaseName = ((PhaseName)phaseNames.elementAt(phaseNameNum)).name;
            alreadyAdded = false;
            for(int phaseNum = 0; phaseNum < phases.size(); phaseNum++) {
                seismicPhase = (SeismicPhase)phases.elementAt(phaseNum);
                if(seismicPhase.name.equals(tempPhaseName)) {
                    phases.removeElementAt(phaseNum);
                    if(seismicPhase.sourceDepth == depth
                            && seismicPhase.tMod.equals(tModDepth)) {
                        // ok so copy to newPhases
                        newPhases.addElement(seismicPhase);
                        alreadyAdded = true;
                        if(verbose) {
                            Alert.info(seismicPhase.toString());
                        }
                        break;
                    }
                }
            }
            if(!alreadyAdded) {
                // didn't find it precomputed, so recalculate
                try {
                    seismicPhase = new SeismicPhase(tempPhaseName, tModDepth);
                    seismicPhase.setDEBUG(DEBUG);
                    seismicPhase.init();
                    newPhases.addElement(seismicPhase);
                    if(verbose) {
                        Alert.info(seismicPhase.toString());
                    }
                } catch(TauModelException e) {
                    Alert.warning("Error with phase=" + tempPhaseName,
                                  e.getMessage() + "\nSkipping this phase");
                } finally {
                    if(verbose) {
                        Alert.info("-----------------");
                    }
                }
            }
        }
        phases = newPhases;
    }

    public void printResult(DataOutputStream dos) throws IOException {
        Writer s = new BufferedWriter(new OutputStreamWriter(dos));
        printResult(s);
        s.flush();
    }

    public void printResult(Writer out) throws IOException {
        Arrival currArrival;
        int maxNameLength = 5;
        int maxPuristNameLength = 5;
        for(int j = 0; j < arrivals.size(); j++) {
            if(((Arrival)arrivals.elementAt(j)).name.length() > maxNameLength) {
                maxNameLength = ((Arrival)arrivals.elementAt(j)).name.length();
            }
            if(((Arrival)arrivals.elementAt(j)).puristName.length() > maxPuristNameLength) {
                maxPuristNameLength = ((Arrival)arrivals.elementAt(j)).puristName.length();
            }
        }
        double moduloDist;
        Format phaseFormat = new Format("%-" + maxNameLength + "s");
        Format phasePuristFormat = new Format("%-" + maxPuristNameLength + "s");
        if(!(onlyPrintRayP || onlyPrintTime)) {
            out.write("\nModel: " + modelName + "\n");
            out.write("Distance   Depth   " + phaseFormat.form("Phase")
                    + "   Travel    Ray Param   Purist    Purist\n");
            out.write("  (deg)     (km)   " + phaseFormat.form("Name ")
                    + "   Time (s)  p (s/deg)  Distance   Name\n");
            for(int i = 0; i < maxNameLength + maxPuristNameLength + 54; i++) {
                out.write("-");
            }
            out.write("\n");
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.elementAt(j);
                out.write(outForms.formatDistance(currArrival.getModuloDistDeg())
                        + outForms.formatDepth(depth) + "   ");
                out.write(phaseFormat.form(currArrival.name));
                out.write("  "
                        + outForms.formatTime(currArrival.time)
                        + "  "
                        + outForms.formatRayParam(Math.PI / 180.0
                                * currArrival.rayParam) + "   ");
                out.write(outForms.formatDistance(currArrival.getDistDeg()));
                if(currArrival.name.equals(currArrival.puristName)) {
                    out.write("  = ");
                } else {
                    out.write("  * ");
                }
                out.write(phasePuristFormat.form(currArrival.puristName) + "\n");
            }
        } else if(onlyPrintTime) {
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.elementAt(j);
                out.write(String.valueOf((float)(currArrival.time)) + " ");
            }
            out.write("\n");
        } else if(onlyPrintRayP) {
            for(int j = 0; j < arrivals.size(); j++) {
                currArrival = (Arrival)arrivals.elementAt(j);
                out.write(String.valueOf((float)(Math.PI / 180.0 * currArrival.rayParam))
                        + " ");
            }
            out.write("\n");
        }
        out.write("\n");
    }

    /**
     * preforms intialization of the tool. Properties are queried for the the
     * default model to load, source depth to use, phases to use, etc. Note that
     * because of the IO inherent in these operations, this method is not
     * appropriate for Applets. Applets should load TauModels themselves and use
     * the setTauModel(TauModel) method.
     */
    public void init() throws IOException {
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
                                                             "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS"));
                    }
                }
            } else {
                parsePhaseList(toolProps.getProperty("taup.phase.list",
                                                     "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS"));
            }
        }
        depth = Double.valueOf(toolProps.getProperty("taup.source.depth", "0.0"))
                .doubleValue();
        if(tMod == null
                || tMod.sMod.vMod.getModelName() != toolProps.getProperty("taup.model.name",
                                                                          "iasp91")) {
            modelName = toolProps.getProperty("taup.model.name", "iasp91");
            try {
                readTauModel();
            } catch(TauModelException ee) {
                Alert.error("Caught TauModelException", ee.getMessage());
            } catch(FileNotFoundException ee) {
                Alert.error("Can't find saved model file for model "
                        + modelName + ".", "");
                System.exit(1);
            } catch(InvalidClassException ee) {
                Alert.error("Model file "
                                    + modelName
                                    + " is not compatible with the current version.",
                            "Recreate using taup_create.");
                System.exit(1);
            }
        }
        if(outFile != null && outFile.length() != 0) {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
        } else {
            dos = new DataOutputStream(System.out);
        }
    }

    public void printHelp() {
        Alert.info("Enter:\nh for new depth\nr to recalculate\n"
                + "p to append phases, \nc to clear phases\n"
                + "l to list phases\n"
                + "s for new station lat lon\ne for new event lat lon\n"
                + "a for new azimuth\nb for new back azimuth\n"
                + "m for new model or \nq to quit.\n");
    }

    public void start() throws IOException, TauModelException, TauPException {
        boolean didDepthCorrect;
        if((degrees != Double.MAX_VALUE || (stationLat != Double.MAX_VALUE
                && stationLon != Double.MAX_VALUE
                && eventLat != Double.MAX_VALUE && eventLon != Double.MAX_VALUE))) {
            /* enough info given on cmd line, so just do one calc. */
            if(degrees == Double.MAX_VALUE) {
                degrees = SphericalCoords.distance(stationLat,
                                                   stationLon,
                                                   eventLat,
                                                   eventLon);
                azimuth = SphericalCoords.azimuth(eventLat,
                                                  eventLon,
                                                  stationLat,
                                                  stationLon);
                backAzimuth = SphericalCoords.azimuth(stationLat,
                                                      stationLon,
                                                      eventLat,
                                                      eventLon);
            }
            depthCorrect(depth);
            calculate(degrees);
            printResult(dos);
        } else {
            /* interactive mode... */
            long prevTime = 0;
            long currTime;
            char readMode = 'd';
            double tempDepth = depth;
            depthCorrect(depth);
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
                        depthCorrect(tempDepth);
                        currTime = System.currentTimeMillis();
                        if(verbose) {
                            Alert.info("depthCorrect time="
                                    + (currTime - prevTime));
                        }
                        readMode = 'd';
                        break;
                    case 'd':
                        // new distance or option
                        System.out.print("Enter Distance or Option [hrpclseabmq]: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            degrees = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("degrees=" + degrees);
                            }
                            calculate(degrees);
                            printResult(dos);
                        } else {
                            if(tokenIn.ttype == tokenIn.TT_EOF
                                    || (tokenIn.ttype == tokenIn.TT_WORD && (tokenIn.sval.equalsIgnoreCase("q")
                                            || tokenIn.sval.equalsIgnoreCase("quit")
                                            || tokenIn.sval.equalsIgnoreCase("exit") || tokenIn.sval.equalsIgnoreCase("bye")))) {
                                readMode = 'q';
                            } else if(tokenIn.ttype == tokenIn.TT_WORD) {
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
                        if(degrees != Double.MAX_VALUE) {
                            calculate(degrees);
                            printResult(dos);
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
                            output += ((PhaseName)phaseNames.elementAt(i)).name;
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
                        } else if(degrees == Double.MAX_VALUE) {
                            readMode = 'd';
                        } else {
                            calculate(degrees);
                            printResult(dos);
                        }
                        readMode = 'd';
                        break;
                    case 'b':
                        //event to station back azimuth (ie station to event
                        // azimuth)
                        System.out.print("Enter back azimuth: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            backAzimuth = tokenIn.nval;
                            eventLat = Double.MAX_VALUE;
                            eventLon = Double.MAX_VALUE;
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
                        } else if(degrees == Double.MAX_VALUE) {
                            readMode = 'd';
                        } else {
                            calculate(degrees);
                            printResult(dos);
                        }
                        readMode = 'd';
                        break;
                    case 'e':
                        // event lat and lon
                        System.out.print("Enter event lat and lon: ");
                        tokenIn.nextToken();
                        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                            eventLat = tokenIn.nval;
                            if(DEBUG) {
                                Alert.info("eventLat=" + eventLat);
                            }
                            tokenIn.nextToken();
                            if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                                eventLon = tokenIn.nval;
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
                            degrees = SphericalCoords.distance(stationLat,
                                                               stationLon,
                                                               eventLat,
                                                               eventLon);
                            azimuth = SphericalCoords.azimuth(eventLat,
                                                              eventLon,
                                                              stationLat,
                                                              stationLon);
                            backAzimuth = SphericalCoords.azimuth(stationLat,
                                                                  stationLon,
                                                                  eventLat,
                                                                  eventLon);
                            calculate(degrees);
                            printResult(dos);
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
                            degrees = SphericalCoords.distance(stationLat,
                                                               stationLon,
                                                               eventLat,
                                                               eventLon);
                            azimuth = SphericalCoords.azimuth(eventLat,
                                                              eventLon,
                                                              stationLat,
                                                              stationLon);
                            backAzimuth = SphericalCoords.azimuth(stationLat,
                                                                  stationLon,
                                                                  eventLat,
                                                                  eventLon);
                            calculate(degrees);
                            printResult(dos);
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
                                depthCorrect(depth);
                            } catch(FileNotFoundException e) {
                                Alert.warning("I can't find model file "
                                        + modelName, "Still using model "
                                        + oldModelName + ".");
                                modelName = oldModelName;
                                tMod = oldTMod;
                                tModDepth = oldTModDepth;
                            } catch(InvalidClassException ee) {
                                Alert.warning("Model file "
                                                      + modelName
                                                      + " is not compatible with the current version.",
                                              "Recreate using taup_create. Still using model "
                                                      + oldModelName + ".");
                                modelName = oldModelName;
                                tMod = oldTMod;
                                tModDepth = oldTModDepth;
                            }
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

    public void destroy() throws IOException {
        if(dos != null) {
            dos.close();
        }
    }

    public void printStdUsageHead() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                                        className.length());
        Alert.info("Usage: " + className.toLowerCase() + " [arguments]");
        Alert.info("  or, for purists, java " + this.getClass().getName()
                + " [arguments]");
        Alert.info("\nArguments are:");
    }

    /** Prints the command line arguments common to all TauP tools. */
    public void printStdUsage() {
        printStdUsageHead();
        Alert.info("-ph phase list     -- comma separated phase list\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n"
                + "-h depth           -- source depth in km\n\n"
                + "Distance is given by:\n\n"
                + "-deg degrees       -- distance in degrees,\n"
                + "-km kilometers     -- distance in kilometers,\n"
                + "                      assumes radius of earth is 6371km,\n\n"
                + "or by giving the station and event latitude and lonitude,\n"
                + "                      assumes a spherical earth,\n\n"
                + "-sta[tion] lat lon -- sets the station latitude and longitude\n"
                + "-evt       lat lon -- sets the event latitude and longitude\n\n");
    }

    public void printStdUsageTail() {
        Alert.info("\n-o outfile         -- output is redirected to \"outfile\"\n"
                + "-debug             -- enable debugging output\n"
                + "-verbose           -- enable verbose output\n"
                + "-version           -- print the version\n"
                + "-help              -- print this out, but you already know that!\n");
    }

    public void printUsage() {
        printStdUsage();
        Alert.info("-rayp              -- only output the ray parameter\n"
                + "-time              -- only output travel time");
        printStdUsageTail();
    }

    /**
     * Allows TauP_Time to run as an application. Creates an instance of
     * TauP_Time. .
     */
    public static void main(String[] args) throws FileNotFoundException,
            IOException, StreamCorruptedException, ClassNotFoundException,
            OptionalDataException {
        BasicConfigurator.configure();
        try {
            long prevTime = 0;
            long currTime;
            prevTime = System.currentTimeMillis();
            TauP_Time tauPTime = new TauP_Time();
            String[] noComprendoArgs = tauPTime.parseCmdLineArgs(args);
            if(noComprendoArgs.length > 0) {
                for(int i = 0; i < noComprendoArgs.length; i++) {
                    if(noComprendoArgs[i].equals("-help")
                            || noComprendoArgs[i].equals("-version")) {
                        System.exit(0);
                    }
                }
                String outStringA = "I don't understand the following arguments, continuing:";
                String outStringB = "";
                for(int i = 0; i < noComprendoArgs.length; i++) {
                    outStringB += noComprendoArgs[i] + " ";
                }
                Alert.warning(outStringA, outStringB);
                noComprendoArgs = null;
            }
            currTime = System.currentTimeMillis();
            prevTime = System.currentTimeMillis();
            tauPTime.init();
            currTime = System.currentTimeMillis();
            if(tauPTime.DEBUG) {
                Alert.info("taup model read time=" + (currTime - prevTime));
            }
            tauPTime.start();
            tauPTime.destroy();
        } catch(TauModelException e) {
            Alert.error("Caught TauModelException", e.getMessage());
            e.printStackTrace();
        } catch(TauPException e) {
            Alert.error("Caught TauPException", e.getMessage());
            e.printStackTrace();
        }
    }
}