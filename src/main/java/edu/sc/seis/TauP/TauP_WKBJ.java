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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import edu.sc.seis.seisFile.mseed3.FDSNSourceId;
import edu.sc.seis.seisFile.mseed3.MSeed3Record;
import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import org.json.JSONArray;
import org.json.JSONObject;

public class TauP_WKBJ extends TauP_Time {

    /**
     * deltaT of the seismogram, default is .05 which gives 20 sps.
     */
    protected double deltaT = .05;

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
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                SeismicPhase phase = phaseList.get(phaseNum);
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
                allArrivals.addAll(phaseArrivals);
            }
            Arrival first = Arrival.getEarliestArrival(allArrivals);
            double firstTime = first == null ? 0 : first.getTime();
            Arrival last = Arrival.getLatestArrival(allArrivals);
            double lastTime = last == null ? 0 : last.getTime();
            //int startTime = (int) (Math.round(firstTime) - 120);
            int startTime = 0;
            double maxTime = lastTime - startTime + 200;
            int numSamples = (int)(Math.ceil(maxTime / getDeltaT())) ;
            float[] radial = new float[numSamples];
            float[] vertical = new float[numSamples];
            float[] transverse = new float[numSamples];

            for (Arrival arrival : allArrivals) {
                int timeIdx = (int) Math.round((arrival.getTime() - startTime)/ getDeltaT());
                double psvAmpFactor = arrival.getAmplitudeFactorPSV();
                double shAmpFactor = arrival.getAmplitudeFactorSH();
                double incidentAngle = arrival.getIncidentAngle();
                double rotateAngle = 0;
                if ( ! arrival.getPhase().finalSegmentIsPWave()) {
                    rotateAngle = 90;
                }
                incidentAngle += rotateAngle;
                transverse[timeIdx] += shAmpFactor;
                radial[timeIdx] += psvAmpFactor * Math.cos(incidentAngle*Math.PI/180);
                vertical[timeIdx] += psvAmpFactor * Math.sin(incidentAngle*Math.PI/180);
            }

            String staCode = "S"+degrees;
            if (staCode.length()> 8) { staCode = staCode.substring(8);}
            spikeRecords.add(packageMSeed3(vertical, staCode, "SP", "Z", startTime ));
            spikeRecords.add(packageMSeed3(radial, staCode, "SP", "R", startTime ));
            spikeRecords.add(packageMSeed3(transverse, staCode, "SP", "T", startTime ));
        }
        return spikeRecords;
    }

    public List<MSeed3Record> packageMSeed3(float[] vertical, float[] radial, float[] transverse, String staCode,
                                            int startSecOffset) {
        List<MSeed3Record> mSeed3Records = new ArrayList<MSeed3Record>();
        mSeed3Records.add(packageMSeed3(vertical, staCode, "W", "Z", startSecOffset ));
        mSeed3Records.add(packageMSeed3(radial, staCode, "W", "R", startSecOffset ));
        mSeed3Records.add(packageMSeed3(transverse, staCode, "W", "T", startSecOffset ));
        return mSeed3Records;
    }

    public MSeed3Record packageMSeed3(float[] data, String staCode,
                                        String sourceCode, String subsourceCode,
                                        int startSecOffset) {
        MSeed3Record ms3RecZ = new MSeed3Record();
        ms3RecZ.setSourceId(new FDSNSourceId("XX", staCode, "00",
                "B", sourceCode, subsourceCode));
        ms3RecZ.setSampleRatePeriod(-1*getDeltaT());
        ms3RecZ.setTimeseries(data);
        ZonedDateTime startDT = ms3RecZ.getStartDateTime().plusSeconds(startSecOffset);
        ms3RecZ.setStartDateTime(startDT);
        return ms3RecZ;
    }

    public static final int RAD_IDX = 0;
    public static final int TRANS_IDX = 1;
    public static final int VERT_IDX = 2;

    public List<MSeed3Record> calcWKBJ(List<Double> degreesList) throws TauPException, IOException {
        validateArguments();
        depthCorrect();
        clearArrivals();
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<MSeed3Record> spikeRecords = new ArrayList<MSeed3Record>();
        float momentMag = 4;
        float[][] sourceTerm = effectiveSourceTerm( momentMag, (float) deltaT,  1000);
        Instant sourceTime = new MSeed3Record().getStartInstant();
        for (double degrees : degreesList) {
            List<Arrival> allArrivals = new ArrayList<Arrival>();
            for (int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                SeismicPhase phase = phaseList.get(phaseNum);
                List<Arrival> phaseArrivals = phase.calcTime(degrees);
                allArrivals.addAll(phaseArrivals);
            }

            Arrival first = Arrival.getEarliestArrival(allArrivals);
            double firstTime = first == null ? 0 : first.getTime();
            Arrival last = Arrival.getLatestArrival(allArrivals);
            double lastTime = last == null ? 0 : last.getTime();
            int startTime = (int) (Math.round(firstTime) - 10);
            double maxTime = lastTime - startTime + 200;
            int numSamples = (int)(Math.ceil(maxTime / getDeltaT())) ;

            float[][] theta_rtz = calcThetaTimeseriesRTZ(degrees, allArrivals, startTime, getDeltaT(), numSamples);
            float[][] rtz = calcThetaTimeseriesRTZ(degrees, allArrivals, startTime, getDeltaT(), numSamples);
            rtz[RAD_IDX] = dumbconvolve(rtz[RAD_IDX], sourceTerm[RAD_IDX]);
            rtz[TRANS_IDX] = dumbconvolve(rtz[TRANS_IDX], sourceTerm[TRANS_IDX]);
            rtz[VERT_IDX] = dumbconvolve(rtz[VERT_IDX], sourceTerm[VERT_IDX]);
            //startTime += (int) (Math.round(sourceTerm[VERT_IDX].length*getDeltaT()));
            String staCode = "W"+degrees;
            if (staCode.length()> 8) { staCode = staCode.substring(8);}
            JSONObject stationEH = new JSONObject();
            stationEH.put("la", 0);
            stationEH.put("lo", 0);
            stationEH.put("el", 0);
            stationEH.put("dp", 0);
            JSONObject quakeEH = new JSONObject();
            JSONObject originEH = new JSONObject();
            originEH.put("tm", MSeed3Record.getDefaultStartTime().format(DateTimeFormatter.ISO_INSTANT));
            originEH.put("la", 0);
            originEH.put("lo", degrees);
            originEH.put("dp", getSourceDepth());
            quakeEH.put("or", originEH);
            JSONObject pathEH = new JSONObject();
            pathEH.put("gcarc", degrees);
            JSONObject bagEH = new JSONObject();
            bagEH.put("st", stationEH);
            bagEH.put("ev", quakeEH);
            bagEH.put("path", pathEH);
            TauP_SetMSeed3.insertMarkers(bagEH, allArrivals, sourceTime);
            JSONObject eh = new JSONObject();
            eh.put("bag", bagEH);
            //eh.put("taup", TauP_Time.resultAsJSONObject(getTauModelName(), getSourceDepth(), getReceiverDepth(), getPhaseNames(), allArrivals));
            spikeRecords.addAll(packageMSeed3(rtz[VERT_IDX], rtz[RAD_IDX], rtz[TRANS_IDX], staCode, startTime));
            spikeRecords.add(packageMSeed3(theta_rtz[VERT_IDX], staCode, "THETA", "Z", startTime));
            for(MSeed3Record msr: spikeRecords) {
                msr.setExtraHeaders(eh);
            }

            spikeRecords.add(packageMSeed3(sourceTerm[0], staCode, "XS", "Z", startTime));
            spikeRecords.add(packageMSeed3(sourceTerm[3], staCode, "TR", "Z", startTime));
            spikeRecords.add(packageMSeed3(sourceTerm[4], staCode, "2HS", "Z", startTime));
        }
        return spikeRecords;
    }

    public static float[] dumbconvolve(float[] a, float[] b) {
        float[] out = new float[a.length+b.length];
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < b.length; j++) {
                if (i>=j && i-j<a.length) {
                    out[i] += a[i - j] * b[j];
                }
            }
        }
        return out;
    }

    public static void trapazoid(float[] data, float m, float rise, float dur, float deltaT) {
        int i = 0;
        int riseIdx = (int) Math.ceil((rise)/deltaT);
        int end = (int) Math.ceil((2*rise+dur)/deltaT);
        if (end >= data.length) {
            throw new ArrayIndexOutOfBoundsException("Trapazoid wider than data, "+end+" >= "+data.length);
        }
        for (i = 0; i <= riseIdx; i++) {
            data[i] = m*i/(rise/deltaT);
            data[Math.round(((2*rise+dur)/deltaT)-i)] = m*i/(rise/deltaT);
        }
        for ( ; i < (rise+dur)/deltaT; i++) {
            data[i] = m;
        }
    }
    public static void secondDerivative(float[] data, float deltaT) {
        float p;
        float c = 0;
        float n = data[0];
        for (int i = 0; i < data.length-1; i++) {
            p = c;
            c = n;
            n = data[i+1];
            data[i] = (p -2*c + n)/(deltaT*deltaT);
            if (c != n) {
                System.out.println(i+" => "+p+" "+c+" "+n+" "+data[i]);
            }
        }
        p = c;
        c = n;
        n = 0;
        data[data.length-1] = (p - 2*c + n)/(deltaT*deltaT);
    }

    public static void boxcar(float[] data, float val, float dur, float deltaT) {
        int end = Math.round(dur/deltaT);
        for (int i = 0; i < end; i++) {
            data[i] = val;
        }
    }
    public static void boxcarDerivative(float[] data, float val, float dur, float deltaT) {
        int end = Math.round(dur/deltaT);
        data[0] = val;
        data[end] = -val;
        for (int i = 1; i < end; i++) {
            data[i] = 0;
        }
    }
    public static void heavyside2ndDerivative(float[] data, float val, float deltaT) {
        int offset = 10;
        data[offset] = val/deltaT/deltaT;
        data[offset+1] = -1*data[offset];
    }

    public static float[][] effectiveSourceTerm(float momentMag, float deltaT, int numSamples) {
        float Mo = (float) mw_to_N_m(momentMag);
        Mo = 1;
        float[] radial = new float[numSamples];
        float[] vertical = new float[numSamples];
        float[] transverse = new float[numSamples];
        //boxcar(radial, momentMag, 1, deltaT);
        //boxcar(vertical, momentMag, 1, deltaT);
        //boxcar(transverse, momentMag, 1, deltaT);

        //trapazoid(radial, momentMag, .1f, .8f, deltaT);
        //secondDerivative(radial, deltaT);
        //trapazoid(vertical, momentMag, .1f, .8f, deltaT);
        //secondDerivative(vertical, deltaT);
        //trapazoid(transverse, momentMag, .1f, .8f, deltaT);
        //secondDerivative(transverse, deltaT);
        //
        //boxcarDerivative(radial, momentMag, 1, deltaT);
        //boxcarDerivative(vertical, momentMag, 1, deltaT);
        //boxcarDerivative(transverse, momentMag, 1, deltaT);

        heavyside2ndDerivative(radial, Mo, deltaT);
        heavyside2ndDerivative(vertical, Mo, deltaT);
        heavyside2ndDerivative(transverse, Mo, deltaT);
        float[] heavysideOverSqrtT = new float[numSamples];
        for (int i = 1; i < heavysideOverSqrtT.length; i++) {
            heavysideOverSqrtT[i] = (float) (1.0/( Math.sqrt(i*deltaT)));
        }
        // 1/sqrt(t) => inf at t=0, so put in 2* next value as finite value
        heavysideOverSqrtT[0] = (float) (heavysideOverSqrtT[1]+1.0/Math.sqrt(deltaT));
        float[][] out = new float[5][];
        out[RAD_IDX] = dumbconvolve(radial, heavysideOverSqrtT);
        out[TRANS_IDX] = dumbconvolve(transverse, heavysideOverSqrtT);
        out[VERT_IDX] = dumbconvolve(vertical, heavysideOverSqrtT);
        float[] trap = new float[numSamples];
        //trapazoid(trap, momentMag, .1f, .8f, deltaT);
        heavyside2ndDerivative(trap, Mo, deltaT);
        out[3] = trap;
        float[] trapSecD = new float[numSamples];
        out[4] = heavysideOverSqrtT;
        return out;
    }

    public static float[][] calcThetaTimeseriesRTZ(double degrees, List<Arrival> allArrivals,
                                            int startTime, double deltaT, int numSamples) throws TauPException {
        float[] radial = new float[numSamples];
        float[] vertical = new float[numSamples];
        float[] transverse = new float[numSamples];

        float[][] rtz = new float[3][];
        rtz[0] = radial;
        rtz[1] = transverse;
        rtz[2] = vertical;

        // no arrivals, just return empty arrays of all zeros
        if (allArrivals.size()==0) {
            return rtz;
        }
        // so have at least one arrival
        double radiusEarthMeter = allArrivals.get(0).getPhase().getTauModel().getRadiusOfEarth()*1000;
        for (Arrival arrival : allArrivals) {
            Theta thetaAtX = new Theta(arrival);

            double minRayParam = arrival.getPhase().getMinRayParam();
            double rayParam = thetaAtX.getMaxRayParam();
            double theta = thetaAtX.getTheta(rayParam);
            double nextRayParam = thetaAtX.getStepRayParam(rayParam,
                    deltaT);
            double nextTheta = thetaAtX.getTheta(nextRayParam);
            int n = 0;
            try {
                while (nextRayParam >= minRayParam) {
                    n = (int) Math.round((theta - startTime)
                            / deltaT);
                    if (n >= 0 && n < numSamples) {
                        try {
                            Arrival thetaArrival = arrival.getPhase().shootRay(rayParam);
                            double psvAmpFactor = thetaArrival.getReflTransPSV();
                            double incidentAngle = thetaArrival.getIncidentAngle();
                            double transverseAmpFactor = thetaArrival.getReflTransSH();
                            double rotateAngle = 0;
                            if ( ! thetaArrival.getPhase().finalSegmentIsPWave()) {
                                rotateAngle = 90;
                            }
                            incidentAngle += rotateAngle;
                            double radialReflTrans = psvAmpFactor * Math.cos(incidentAngle*Math.PI/180);
                            double verticalReflTrans = psvAmpFactor * Math.sin(incidentAngle*Math.PI/180);
                            double drp_s_m = (rayParam- nextRayParam)/radiusEarthMeter;
                            double rp_s_m = rayParam/radiusEarthMeter;
                            radial[n] += (float) (Math.sqrt(rp_s_m) * radialReflTrans * drp_s_m);
                            vertical[n] += (float) (Math.sqrt(rp_s_m) * verticalReflTrans * drp_s_m);
                            transverse[n] += (float) (Math.sqrt(rp_s_m) * transverseAmpFactor * drp_s_m);
                        } catch (SlownessModelException e) {
                            // thetaArrival doesn't exist, so contribution is zero for this ray param?
                        }
                    }
                    rayParam = nextRayParam;
                    theta = nextTheta;
                    nextRayParam = thetaAtX.getStepRayParam(rayParam,
                            deltaT);
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
        // 1/(pi*sqrt(2x) term
        // wrong as some phases may go long way around, but for now...
        for (int i = 0; i < radial.length; i++) {
            double dist_m = degrees*Math.PI/180*radiusEarthMeter;
            radial[i] = (float) (radial[i] * 1.0/(Math.PI*Math.sqrt(2*dist_m)));
            transverse[i] = (float) (transverse[i] * 1.0/(Math.PI*Math.sqrt(2*dist_m)));
            vertical[i] = (float) (vertical[i] * 1.0/(Math.PI*Math.sqrt(2*dist_m)));

        }
        return rtz;
    }

    /**
     *
     * Mw to Mo conversion from Lay and Wallace p. 384, I assumed that Mo is in
     * newton meters hence multiply by 10^7 to change to dyne cm
     * (1 Newton = 10^5 dynes and 1 m = 10^2 cm)
     *
     * @return
     */
    public static double mw_to_N_m(double Mw) {
        double scalar_moment_N_m = Math.pow(10, (Mw + 10.73) * 1.5 - 7.0);
        return scalar_moment_N_m;
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
