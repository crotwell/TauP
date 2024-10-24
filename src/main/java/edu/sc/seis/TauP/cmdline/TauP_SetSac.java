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
package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.GeodeticArgs;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.cmdline.args.PhaseArgs;
import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.sac.SacConstants;
import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

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
        description = "Set headers to travel times of phases using depth and distance from SAC files.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_SetSac extends TauP_AbstractPhaseTool {

    protected List<String> sacFileNames = new ArrayList<>();

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

    @CommandLine.Parameters(description = "SAC files to process",
            paramLabel = "sacfile"
    )
    public void setSacFileNames(String[] sacFileNames) {
        this.sacFileNames = new ArrayList<>();
        this.sacFileNames.addAll(Arrays.asList(sacFileNames));
    }

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();

    protected TauP_SetSac() {
        super(null);
    }

    protected void setSacVarNums() throws PhaseParseException, IOException {
        boolean[] headersUsed = new boolean[11]; // A header is 10
        for(PhaseName pn : parsePhaseNameList()) {
            for(int t : pn.sacTNumTriplication) {
                if (t != SKIP_HEADER) {
                    headersUsed[t] = true;
                }
            }
        }
        int j=0;
        for(PhaseName pn : parsePhaseNameList()) {
            if(pn.sacTNumTriplication.isEmpty()) {
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

    @Override
    public String getOutputFormat() {
        return OutputTypes.SAC;
    }

    public void init() throws TauPException {
        super.init();
        try {
            setSacVarNums();
        } catch (IOException e) {
            throw new TauPException(e);
        }
    }

    public void start() throws IOException, TauPException {
        if (sacFileNames.isEmpty()) {
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

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {

    }

    public void processSacFile(File f) throws IOException, TauPException {

        if (f.isDirectory()) {
            File[] subfiles = f.listFiles();
            for (File subfile : subfiles) {
                if (subfile.getName().startsWith(".")) {
                    continue;
                }
                processSacFile(subfile);
            }
            return;
        }
        // regular file, hopefully
        try {
            SacTimeSeries sacFile = SacTimeSeries.read(f);
            processSacTimeSeries(sacFile, f.getName());
            sacFile.write(f);
        } catch (SetSacException e) {
            System.err.println(e.getMessage()+", skipping.");
        }
    }

    public void processSacTimeSeries(SacTimeSeries sacFile, String filenameForError) throws TauPException {
        SacHeader header = sacFile.getHeader();
        if(SacConstants.isUndef(header.getEvdp())) {
            throw new SetSacException("Depth not set in "
                    + filenameForError );
        }
        if(SacConstants.isUndef(header.getO())) {
            throw new SetSacException("O marker not set in "
                    + filenameForError );
        }
        RayCalculateable rayCalculateable;
        if(! SacConstants.isUndef(header.getGcarc())) {
            if(isVerbose()) {
                System.err.println("Using gcarc: " + header.getGcarc());
            }
            rayCalculateable = DistanceRay.ofDegrees(header.getGcarc());
        } else if(! SacConstants.isUndef(header.getDist())) {
            if(isVerbose()) {
                System.err.println("Using dist: " + header.getDist());
            }
            rayCalculateable = DistanceRay.ofKilometers(header.getDist());
        } else if( ! SacConstants.isUndef(sacFile.getHeader().getStla()) && ! SacConstants.isUndef(sacFile.getHeader().getStlo())
                && ! SacConstants.isUndef(sacFile.getHeader().getEvla()) && ! SacConstants.isUndef(sacFile.getHeader().getEvlo())) {
            if(isVerbose()) {
                System.err.println("Using stla,stlo, evla,evlo to calculate");
            }
            Alert.warning("Warning: Sac header gcarc is not set in "+filenameForError+",",
                          "using lat and lons to calculate distance.");
            if (geodeticArgs.isGeodetic()) {
                rayCalculateable = DistanceRay.ofGeodeticStationEvent(
                        new Location(header.getStla(), header.getStlo()),
                        new Location(header.getEvla(), header.getEvlo(), header.getEvdp()),
                        DistAz.wgs85_flattening
                );
            } else {
                rayCalculateable = DistanceRay.ofStationEvent(
                        new Location(header.getStla(), header.getStlo()),
                        new Location(header.getEvla(), header.getEvlo(), header.getEvdp())
                );
            }
        } else {
            /* can't get a distance, skipping */
            throw new SetSacException("Can't get a distance, all distance fields are undef in "+filenameForError);
        }
        if(modelArgs.getSourceDepths().size()!= 1 || !((evdpkm && modelArgs.getSourceDepths().get(0) == header.getEvdp()) || (!evdpkm && modelArgs.getSourceDepths().get(0) == 1000 * header.getEvdp()))) {
            if(!evdpkm && header.getEvdp() != 0 && header.getEvdp() < 1000.0) {
                Alert.warning("Sac header evdp is < 1000 in "
                                      + filenameForError,
                              "If the depth is in kilometers instead of meters "
                                      + "(default), you should use the -evdpkm flag");
            }
            if(evdpkm) {
                setSingleSourceDepth(header.getEvdp());
            } else {
                setSingleSourceDepth(header.getEvdp() / 1000.0);
            }
        }
        if(isVerbose()) {
            System.err.println(filenameForError + " searching for " + PhaseArgs.getPhaseNamesAsString(parsePhaseNameList()));
        }
        for(int j = getSeismicPhases().size() - 1; j >= 0; j--) {
            SeismicPhase phase = getSeismicPhases().get(j);
            List<Arrival> arrivalList = rayCalculateable.calculate(phase);
            int phaseNum = -1;
            for(int pnidx = parsePhaseNameList().size() - 1; pnidx >= 0; pnidx--) {
                if(phase.getName().equals(parsePhaseNameList().get(pnidx).name)) {
                    phaseNum = pnidx;
                    break;
                }
            }
            PhaseName pn = parsePhaseNameList().get(phaseNum);
            int tripNum = 0;
            for (int tripHeader: pn.sacTNumTriplication) {
                if (tripNum >= arrivalList.size()) {
                    break;
                }
                Arrival tripArrival = arrivalList.get(tripNum);
                if (tripHeader != SKIP_HEADER) {
                    if (isVerbose()) {
                        System.err.println(
                                " phase found " + pn.name + " = "
                                + tripArrival.getName() + " trip(" + tripNum + ")"
                                + " -> t"
                                + tripHeader
                                + ", travel time="
                                + (float) tripArrival.getTime());
                    }
                    setSacTHeader(sacFile, tripHeader, tripArrival);
                } else {
                    if (isVerbose()) {
                        System.err.println(
                                " phase found " + pn.name + " = "
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
