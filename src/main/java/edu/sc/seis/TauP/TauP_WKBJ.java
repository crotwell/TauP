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
/**
 * TauP_WKBJ.java
 * 
 * 
 * 
 * 
 * @author Philip Crotwell
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 */
package edu.sc.seis.TauP;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import edu.sc.seis.seisFile.mseed3.FDSNSourceId;
import edu.sc.seis.seisFile.mseed3.MSeed3Record;
import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;

public class TauP_WKBJ extends TauP_Time {

    /**
     * deltaT of the seismogram, default is .05 which gives 20 sps.
     */
    protected double deltaT = .05;

    /** number of samples in the seismogram. Default is 100. */
    protected int numSamples = 1000;

    /**
     * start time of the seismogram relative to the origin time. default is 0.
     */
    protected double startTime = 0;

    public TauP_WKBJ() {
        super();
    }

    public TauP_WKBJ(TauModel tMod) throws TauModelException {
        super(tMod);
    }

    public TauP_WKBJ(String modelName) throws TauModelException {
        super(modelName);
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[] { TauP_Tool.MS3 };
    }

    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(TauP_Tool.MS3);
    }

    /**
     * Get the value of deltaT.
     * 
     * @return Value of deltaT.
     */
    public double getDeltaT() {
        return deltaT;
    }

    /**
     * Set the value of deltaT.
     * 
     * @param v
     *            Value to assign to deltaT.
     */
    public void setDeltaT(double v) {
        this.deltaT = v;
    }

    /**
     * Get the value of numSamples.
     * 
     * @return Value of numSamples.
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * Set the value of numSamples.
     * 
     * @param v
     *            Value to assign to numSamples.
     */
    public void setNumSamples(int v) {
        this.numSamples = v;
    }

    /**
     * Get the value of startTime.
     * 
     * @return Value of startTime.
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Set the value of startTime.
     * 
     * @param v
     *            Value to assign to startTime.
     */
    public void setStartTime(double v) {
        this.startTime = v;
    }

    @Override
    public List<Arrival> calculate(List<Double> degreesList) throws TauPException {
        try {
            List<Arrival> arrivals = super.calculate(degreesList);
            List<MSeed3Record> spikeRecords = calcWKBJ(degreesList);
            //List<MSeed3Record> spikeRecords = calcSpikes(degreesList);

            File outMSeed3File = new File("taup_spikes.ms3");
            setOutFileBase("taup_spikes");
            DataOutputStream dos = getOutputStream();
            for (MSeed3Record ms3 : spikeRecords) {
                dos.write(ms3.asByteBuffer().array());
            }
            dos.close();
            return sortArrivals(arrivals);
        } catch(IOException e) {
            throw new TauPException(e);
        }
    }

    @Override
    public List<Arrival> calcEventStation(Double[] evloc, List<Double[]> staloc) throws TauPException {
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

    public List<MSeed3Record> calcSpikes(List<Double> degreesList) throws TauPException, IOException {

        // testing....
        //setDeltaT(.10);


        validateArguments();
        depthCorrect();
        clearArrivals();
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<Arrival> allArrivals = new ArrayList<Arrival>();
        List<MSeed3Record> spikeRecords = new ArrayList<MSeed3Record>();

        for (double degrees : degreesList) {
            System.out.println("In calcWKBJ spikes for " + degrees + " degrees.");
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                SeismicPhase phase = phaseList.get(phaseNum);
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
                allArrivals.addAll(phaseArrivals);
            }
            double maxTime = 0;
            for (Arrival arrival : allArrivals) {
                if (maxTime < arrival.getTime()) {
                    maxTime = arrival.getTime();
                }
            }
            int numSamples = (int)(Math.ceil(maxTime / getDeltaT())) + 100;
            float[] seismogramPoints = new float[numSamples];

            for (Arrival arrival : allArrivals) {
                int timeIdx = (int) Math.round(arrival.getTime() / getDeltaT());
                System.out.println(arrival.getTime()+" "+arrival.getAmplitudeFactor()+" idx:"+timeIdx);
                seismogramPoints[timeIdx] += arrival.getAmplitudeFactor();
            }

            MSeed3Record ms3Rec = new MSeed3Record();
            String staCode = "D"+degrees;
            if (staCode.length()> 8) { staCode = staCode.substring(8);}
            ms3Rec.setSourceId(new FDSNSourceId("XX", staCode, "00", "B", "X", "Z"));
            ms3Rec.setSampleRate(getDeltaT());
            ms3Rec.setTimeseries(seismogramPoints);
            spikeRecords.add(ms3Rec);
        }
        return spikeRecords;
    }

    public List<MSeed3Record> calcWKBJ(List<Double> degreesList) throws TauPException, IOException {
        validateArguments();
        depthCorrect();
        clearArrivals();
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<MSeed3Record> spikeRecords = new ArrayList<MSeed3Record>();
        for (double degrees : degreesList) {
            System.out.println("In calcWKBJ for " + degrees + " degrees.");
            List<Arrival> allArrivals = new ArrayList<Arrival>();
            float[] seismogramPoints = new float[numSamples];
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                SeismicPhase phase = phaseList.get(phaseNum);
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
                allArrivals.addAll(phaseArrivals);
            }
            for (Arrival arrival : allArrivals) {
                double reflTrans = arrival.getReflTrans();
                Theta thetaAtX = new Theta(arrival.getPhase(), arrival.getDist());

                System.out.println("Got Theta");
                double minRayParam = arrival.getPhase().getMinRayParam();
                double rayParam = thetaAtX.getMaxRayParam();
                System.out.println("Got ray param");
                double theta = thetaAtX.getTheta(rayParam);
                System.out.println("Got theta for ray param");
                setStartTime(320);
                double nextRayParam = thetaAtX.getStepRayParam(rayParam,
                        getDeltaT());
                double nextTheta = thetaAtX.getTheta(nextRayParam);
                int n = 0;
                try {
                    while (nextRayParam >= minRayParam) {
                        // System.out.println(n+" "+rayParam+" "+theta+"
                        // "+nextRayParam+" "+nextTheta);
                        n = (int) Math.round((theta - getStartTime())
                                / getDeltaT());
                        if (n >= 0 && n < seismogramPoints.length) {
                            seismogramPoints[n] += (float)(Math.sqrt(rayParam)*reflTrans*(rayParam- nextRayParam));
                            System.out.println(n + "  "
                                    + seismogramPoints[n]);
                        }
                        rayParam = nextRayParam;
                        theta = nextTheta;
                        nextRayParam = thetaAtX.getStepRayParam(rayParam,
                                getDeltaT());
                        if (nextRayParam == -1) {
                            break;
                        }
                        nextTheta = thetaAtX.getTheta(nextRayParam);
                    }

                } catch (ArrayIndexOutOfBoundsException e) {
                    // must have dropped off of end of theta curve
                    throw new TauPException(e);
                }
            }

            MSeed3Record ms3Rec = new MSeed3Record();
            String staCode = "D"+degrees;
            if (staCode.length()> 8) { staCode = staCode.substring(8);}
            ms3Rec.setSourceId(new FDSNSourceId("XX", staCode, "00", "B", "X", "Z"));
            ms3Rec.setSampleRate(getDeltaT());
            ms3Rec.setTimeseries(seismogramPoints);
            spikeRecords.add(ms3Rec);
        }
        return spikeRecords;
    }

    DataOutputStream writer;

    public DataOutputStream getOutputStream() throws IOException {
        if (writer == null) {
            if(!getOutFile().equals("stdout")) {
                writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getOutFile())));
            } else {
                writer = new DataOutputStream(new BufferedOutputStream(System.out));
            }
        }
        return writer;
    }

    public void closeOutputStream() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                // oh well
            }
        }
        writer = null;
    }

    public void setOutputStream(DataOutputStream writer) {
        this.writer = writer;
    }
} // TauP_WKBJ
