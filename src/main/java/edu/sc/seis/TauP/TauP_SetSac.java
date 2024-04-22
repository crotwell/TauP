/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>
 * 
 * Bug reports and comments should be directed to H. Philip Crotwell,
 * crotwell@seis.sc.edu or Tom Owens, owens@seis.sc.edu
 * 
 */
package edu.sc.seis.TauP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.sc.seis.seisFile.sac.SacConstants;
import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import picocli.CommandLine;

/**
 * Calculate times for phases and set sac headers based on gcarc or dist or
 * station lat and lon and event lat and lon.
 * 
 * Note that triplicated phases will cause problems, as there is only one spot
 * to put a time. An improved method would allow a phase to have several t#'s
 * associated with it, so that all arrivals could be marked. Currently however,
 * only the first arrival for a phase name is used.
 * 
 * Warning: I assume the evdp header has depth in meters unless the -evdpkm flag
 * is set, in which case I assume kilometers. This may be a problem for users
 * that improperly use kilometers for the depth units. Due to much abuse of the
 * sac depth header units I output a warning message if the depth appears to be
 * in kilometers, ie it is less than 1000. This can be safely ignored if the event
 * really is less than 1000 meters deep.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 * 
 */
@CommandLine.Command(name = "setsac",
        description = "set headers to travel times of phases using depth and distance from headers",
        usageHelpAutoWidth = true)
public class TauP_SetSac extends TauP_Time {

    protected List<String> sacFileNames = new ArrayList<String>();

    protected boolean evdpkm = false;

    public static final int A_HEADER = 10;
    public static final int SKIP_HEADER = 11;

    public boolean getEvdpkm() {
        return evdpkm;
    }

    @CommandLine.Option(names = "--evdpkm", description = "sac depth header is in km, default is meters")
    public void setEvdpkm(boolean evdpkm) {
        this.evdpkm = evdpkm;
    }

    @CommandLine.Parameters
    public void setSacFileNames(String[] sacFileNames) {
        this.sacFileNames = new ArrayList<String>();
        for(int i = 0; i < sacFileNames.length; i++) {
            this.sacFileNames.add(sacFileNames[i]);
        }
    }

    protected TauP_SetSac() {
        super();
    }

    public TauP_SetSac(TauModel tMod) throws TauModelException {
        super(tMod);
    }

    public TauP_SetSac(String modelName) throws TauModelException {
        super(modelName);
    }

    protected void setSacVarNums() {
        boolean[] headersUsed = new boolean[11]; // A header is 10
        for(int i = 0; i < headersUsed.length; i++) {
            headersUsed[i] = false;
        }
        for(PhaseName pn : phaseNames) {
            for(int t : pn.sacTNumTriplication) {
                if (t != SKIP_HEADER) {
                    headersUsed[t] = true;
                }
            }
        }
        int j=0;
        for(PhaseName pn : phaseNames) {
            if(pn.sacTNumTriplication.size() == 0) {
                // find a j that hasn't been used
                while(j < headersUsed.length && headersUsed[j]){ j++; }
                if(j < 10) {
                    // don't use A header (10) in the automatic header
                    pn.sacTNumTriplication.add(j);
                    headersUsed[j] = true;
                } else { break; }
            }
        }
    }

    public void init() throws TauPException {
        super.init();
        setSacVarNums();
    }

    public void start() throws IOException, TauPException {
        if (sacFileNames.size() == 0) {
            CommandLine.usage(this, System.out);
            return;
        }
        for (String filename : sacFileNames) {
            if(isVerbose()) {
                System.err.println(filename);
            }
            processSacFile(new File(filename));
        }
    }
    
    public void processSacFile(File f) throws IOException, TauPException {
        
        if (f.isDirectory()) {
            File[] subfiles = f.listFiles();
            for (int j = 0; j < subfiles.length; j++) {
                if (subfiles[j].getName().startsWith(".")) {
                    continue;
                }
                processSacFile(subfiles[j]);
            }
            return;
        }
        // regular file, hopefully
        SacTimeSeries sacFile = SacTimeSeries.read(f);
        SacHeader header = sacFile.getHeader();
        if(SacConstants.isUndef(header.getEvdp())) {
            System.err.println("Depth not set in "
                    + f.getName() + ", skipping");
            return;
        }
        if(SacConstants.isUndef(header.getO())) {
            System.err.println("O marker not set in "
                    + f + ", skipping");
            return;
        }
        double deg;
        if(! SacConstants.isUndef(header.getGcarc())) {
            if(isVerbose()) {
                System.err.println("Using gcarc: " + header.getGcarc());
            }
            deg = header.getGcarc();
        } else if(! SacConstants.isUndef(header.getDist())) {
            if(isVerbose()) {
                System.err.println("Using dist: " + header.getDist());
            }
            deg = header.getDist() / 6371.0 * 180.0 / Math.PI;
        } else if( ! SacConstants.isUndef(sacFile.getHeader().getStla()) && ! SacConstants.isUndef(sacFile.getHeader().getStlo())
                && ! SacConstants.isUndef(sacFile.getHeader().getEvla()) && ! SacConstants.isUndef(sacFile.getHeader().getEvlo())) {
            if(isVerbose()) {
                System.err.println("Using stla,stlo, evla,evlo to calculate");
            }
            Alert.warning("Warning: Sac header gcarc is not set,",
                          "using lat and lons to calculate distance.");
            Alert.warning("No ellipticity correction will be applied.",
                          "This may introduce errors. Please see the manual.");
            deg = SphericalCoords.distance(header.getStla(),
                                           header.getStlo(),
                                           header.getEvla(),
                                           header.getEvlo());
        } else {
            /* can't get a distance, skipping */
            Alert.warning("Can't get a distance, all distance fields are undef.",
                          "skipping " + f);
            return;
        }
        if(!((evdpkm && modelArgs.getSourceDepth() == header.getEvdp()) || (!evdpkm && modelArgs.getSourceDepth() == 1000 * header.getEvdp()))) {
            if(!evdpkm && header.getEvdp() != 0 && header.getEvdp() < 1000.0) {
                Alert.warning("Sac header evdp is < 1000 in "
                                      + f,
                              "If the depth is in kilometers instead of meters "
                                      + "(default), you should use the -evdpkm flag");
            }
            if(evdpkm) {
                setSourceDepth(header.getEvdp());
            } else {
                setSourceDepth(header.getEvdp() / 1000.0);
            }
        }
        if(isVerbose()) {
            System.err.println(f
                    + " searching for " + getPhaseNameString());
        }
        List<Arrival> arrivalList = calculate(deg);
        // calcTime(deg);
        if(isVerbose()) {
            System.err.println(f + " "
                    + arrivalList.size() + " arrivals found.");
        }
        // set arrivals in header, look for triplications if configured in phase name
        List<Arrival> arrivalCopy = new ArrayList<Arrival>();
        arrivalCopy.addAll(arrivalList);
        while (arrivalCopy.size() > 0) {
            int phaseNum = -1;
            Arrival currArrival = arrivalCopy.get(0);
            for(int j = phaseNames.size() - 1; j >= 0; j--) {
                if(currArrival.getName()
                        .equals(((PhaseName)phaseNames.get(j)).name)) {
                    phaseNum = j;
                    break;
                }
            }
            if(phaseNum != -1) {
                PhaseName pn = phaseNames.get(phaseNum);
                int tripNum = 0;
                for (int tripHeader: pn.sacTNumTriplication) {
                    while (tripNum < arrivalCopy.size() && ! arrivalCopy.get(tripNum).getName().equals(pn.name)) {
                        tripNum++;
                    }
                    if (tripNum < arrivalCopy.size()) {
                        Arrival tripArrival = arrivalCopy.get(tripNum);
                        if (tripHeader != SKIP_HEADER) {
                            if (isVerbose()) {
                                System.err.println(f
                                        + " phase found " + pn.name + " = "
                                        + tripArrival.getName() + " trip(" + tripNum + ")"
                                        + " -> t"
                                        + tripHeader
                                        + ", travel time="
                                        + (float) tripArrival.getTime());
                            }
                            setSacTHeader(sacFile, tripHeader, tripArrival);
                        } else {
                            if (isVerbose()) {
                                System.err.println(f
                                        + " phase found " + pn.name + " = "
                                        + tripArrival.getName() + " trip(" + tripNum + ")"
                                        + " -> skip"
                                        + ", travel time="
                                        + (float) tripArrival.getTime());
                            }
                        }
                        tripNum++;
                    }
                }
            }
            List<Arrival> cleanArrivals = new ArrayList<>();
            for (Arrival a : arrivalCopy) {
                if (! a.getName().equals(currArrival.getName())) {
                    cleanArrivals.add(a);
                }
            }
            arrivalCopy = cleanArrivals;
        }
        sacFile.write(f);
    }

    public static void setSacTHeader(SacTimeSeries sacFile,
                                     int headerNum,
                                     Arrival arrival) {
        float arrivalTime = sacFile.getHeader().getO() + (float)arrival.getTime();
        if(headerNum == A_HEADER) {
                // there is no t10, so use that for the A header
            sacFile.getHeader().setA( arrivalTime);
            sacFile.getHeader().setKa( arrival.getName());
                // no place to put the ray param
        } else {
            sacFile.getHeader().setTHeader(headerNum, arrivalTime, arrival.getName());
            sacFile.getHeader().setUserHeader(headerNum, (float)arrival.getRayParam());
        }
    }

    public String getStdUsage() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                                        className.length());
        return "Usage: " + className.toLowerCase() + " [arguments]"
        +"  or, for purists, java "
                + this.getClass().getName() + " [arguments]"
        +"\nArguments are:"
        +"-ph phase list     -- comma separated phase list,\n"
                + "                      use phase-# to specify the sac header,\n"
                + "                      for example, ScS-8 puts ScS in t8\n"
                + "-pf phasefile      -- file containing phases\n\n"
                + "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n";
    }

    public String getUsageTail() {
        return "\n"
                + "--prop [propfile]   -- set configuration properties\n"
                + "--debug             -- enable debugging output\n"
                + "--verbose           -- enable verbose output\n"
                + "--version           -- print the version\n"
                + "--help              -- print this out, but you already know that!\n";
    }

    /**
     * Allows TauP_SetSac to run as an application. Creates an instance of
     * TauP_SetSac. 
     * 
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.SETSAC, args);
    }
}
