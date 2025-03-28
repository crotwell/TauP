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
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import edu.sc.seis.TauP.cmdline.args.SeismogramOutputTypeArgs;
import edu.sc.seis.seisFile.fdsnws.quakeml.*;
import edu.sc.seis.seisFile.mseed3.FDSNSourceId;
import edu.sc.seis.seisFile.mseed3.MSeed3EH;
import edu.sc.seis.seisFile.mseed3.MSeed3EHKeys;
import edu.sc.seis.seisFile.mseed3.MSeed3Record;
import edu.sc.seis.seisFile.mseed3.ehbag.Marker;
import edu.sc.seis.seisFile.mseed3.ehbag.Path;
import picocli.CommandLine;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;
import static edu.sc.seis.TauP.cmdline.args.OutputTypes.MS3;

@CommandLine.Command(name = "spikes",
        description = "Calculate spike seismograms",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_Spikes extends TauP_AbstractRayTool {

    /**
     * deltaT of the seismogram, default is .05 which gives 20 sps.
     */
    protected double deltaT = .05;

    public TauP_Spikes() {
        super(new SeismogramOutputTypeArgs(MS3, "taup_spikes"));
        outputTypeArgs = (SeismogramOutputTypeArgs) abstractOutputTypeArgs;
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
    public void validateArguments() throws TauPException {
        super.validateArguments();
        if (!getOutputFormat().equals(MS3)) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Unsupported Output Format: "+getOutputFormat());
        }
        if (modelArgs.getSourceDepths().size() > 1) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Multiple source depths unsupported: "+modelArgs.getSourceDepths().size());
        }
        sourceArgs.validateArguments();
    }

    @Override
    public void init() throws TauPException {
        super.init();
    }

    @Override
    public void start() throws IOException, TauPException {
        try {

            List<MSeed3Record> allRecords = new ArrayList<>();
            List<MSeed3Record> spikeRecords = calcSpikes(distanceArgs.getDistances());
            allRecords.addAll(spikeRecords);

            setOutFileBase("taup_spikes");
            DataOutputStream dos = getOutputStream();
            for (MSeed3Record ms3 : allRecords) {
                dos.write(ms3.asByteBuffer().array());
            }
            dos.close();
        } catch(IOException e) {
            throw new TauPException(e);
        }
    }

    public List<MSeed3Record> calcSpikes(List<DistanceRay> degreesList) throws TauPException {

        // testing....
        //setDeltaT(.10);


        validateArguments();
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<MSeed3Record> spikeRecords = new ArrayList<>();

        for (DistanceRay distVal : degreesList) {
            if ( ! distVal.hasSourceArgs()) {
                distVal.setSourceArgs(sourceArgs);
            }
            List<Arrival> allArrivals = new ArrayList<>();
            List<MSeed3Record> componentRecords = new ArrayList<>();
            double degrees = distVal.getDegrees(getRadiusOfEarth());
            for (SeismicPhase phase : phaseList) {
                List<Arrival> phaseArrivals = distVal.calculate(phase);
                allArrivals.addAll(phaseArrivals);
            }
            Arrival last = Arrival.getLatestArrival(allArrivals);
            double lastTime = last == null ? 0 : last.getTime();
            //int startTime = (int) (Math.round(firstTime) - 120);
            int startTime = 0;
            double maxTime = lastTime - startTime + 200;
            int numSamples = (int)(Math.ceil(maxTime / getDeltaT())) ;
            float[] radial = new float[numSamples];
            float[] vertical = new float[numSamples];
            float[] transverse = new float[numSamples];
            MSeed3EH eh = createEH(degrees, getOriginTime(), allArrivals);

            for (Arrival arrival : allArrivals) {
                int timeIdx = (int) Math.ceil((arrival.getTime() - startTime)/ getDeltaT());
                double psvAmpFactor = arrival.getAmplitudeFactorPSV();
                if ( ! Double.isFinite(psvAmpFactor)) { psvAmpFactor = 0.0;}
                double shAmpFactor = arrival.getAmplitudeFactorSH();
                if ( ! Double.isFinite(shAmpFactor)) { shAmpFactor = 0.0;}
                double incidentAngle = arrival.getIncidentAngleDegree();
                double rotateAngle = 0;
                if ( ! arrival.getPhase().finalSegmentIsPWave()) {
                    rotateAngle = 90;
                }
                incidentAngle += rotateAngle;
                int width = 2;
                if (deltaT < 1) {
                    width = (int) Math.round(1/deltaT) + 1;
                }
                for (int w = 0; w < width; w++) {

                transverse[timeIdx+w] += shAmpFactor;
                radial[timeIdx+w] += psvAmpFactor * Math.cos(incidentAngle*Math.PI/180);
                vertical[timeIdx+w] += psvAmpFactor * Math.sin(incidentAngle*Math.PI/180);
                }
            }

            String staCode = "S"+degrees;
            if (staCode.length()> 8) { staCode = staCode.substring(8);}
            componentRecords.add(packageMSeed3(vertical, staCode, "SP", "Z", startTime ));
            componentRecords.add(packageMSeed3(radial, staCode, "SP", "R", startTime ));
            componentRecords.add(packageMSeed3(transverse, staCode, "SP", "T", startTime ));
            for (MSeed3Record ms3 : componentRecords) {
                    ms3.setStartDateTime(getOriginTime());
                    ms3.setExtraHeaders(eh.getEH());

            }
            spikeRecords.addAll(componentRecords);
        }
        return spikeRecords;
    }

    public MSeed3EH createEH(double degrees, ZonedDateTime startDateTime, List<Arrival> allArrivals) throws TauPException {
        MSeed3EH eh = new MSeed3EH();

        // assume single source
        double sourceDepth = modelArgs.getSourceDepths().isEmpty() ? 0 : modelArgs.getSourceDepths().get(0);
        Float deg = (float) degrees;
        Float az = distanceArgs.hasAzimuth() ? distanceArgs.getAzimuth().floatValue() : null;
        Float baz = distanceArgs.hasBackAzimuth() ? distanceArgs.getBackAzimuth().floatValue() : null;
        Path path = new Path(deg, az, baz);
        eh.addToBag(path);

        Origin origin = new Origin();
        origin.setDepth(new RealQuantity((float) sourceDepth));
        origin.setTime(new Time(startDateTime.toInstant()));

        float olat = 0;
        float olon = 0;
        if (distanceArgs.hasEventLatLon()) {
            olat = (float) distanceArgs.getEventLatLon().get(0).getLatitude();
            olon = (float) distanceArgs.getEventLatLon().get(0).getLongitude();
        }
        origin.setLatitude(new RealQuantity(olat));
        origin.setLongitude(new RealQuantity(olon));
        Event event = new Event(origin);
        event.setPreferredOriginID(origin.getPublicId());

        Magnitude mag = new Magnitude();
        mag.setMag(new RealQuantity(sourceArgs.getMw()));
        mag.setType("Mw");
        event.setMagnitudeList(List.of(mag));
        event.setPreferredMagnitudeID(mag.getPublicId());
        eh.addToBag(event);
        for (Arrival a : allArrivals) {
            ZonedDateTime arrTime = startDateTime.plusNanos(Math.round(a.getTime() * 1e9));
            String desc = "";
            Marker m = new Marker(a.getName(), arrTime, MSeed3EHKeys.MARKER_MODELED, desc);
            eh.addToBag(m);
        }
        return eh;
    }

    public List<MSeed3Record> packageMSeed3(float[] vertical, float[] radial, float[] transverse, String staCode,
                                            int startSecOffset) {
        List<MSeed3Record> mSeed3Records = new ArrayList<>();
        mSeed3Records.add(packageMSeed3(vertical, staCode, "W", "Z", startSecOffset ));
        mSeed3Records.add(packageMSeed3(radial, staCode, "W", "R", startSecOffset ));
        mSeed3Records.add(packageMSeed3(transverse, staCode, "W", "T", startSecOffset ));
        return mSeed3Records;
    }

    public MSeed3Record packageMSeed3(float[] data, String staCode,
                                        String sourceCode, String subsourceCode,
                                        int startSecOffset) {
        MSeed3Record ms3RecZ = new MSeed3Record();
        String bandCode = "B";
        ms3RecZ.setSourceId(new FDSNSourceId("XX", staCode, "00",
                bandCode, sourceCode, subsourceCode));
        ms3RecZ.setSampleRatePeriod(-1*getDeltaT());
        ms3RecZ.setTimeseries(data);
        ZonedDateTime startDT = ms3RecZ.getStartDateTime().plusSeconds(startSecOffset);
        ms3RecZ.setStartDateTime(startDT);
        return ms3RecZ;
    }

    public static final int RAD_IDX = 0;
    public static final int TRANS_IDX = 1;
    public static final int VERT_IDX = 2;

    /**
     * WARNING: Experimental.
     * <p>
     * Probably doesn't work due to wkbj formalism is flat earth and so doesn't translate to spherical.
     * Possible to recalculate via EFT, but not sure worth it.
     */
    public List<MSeed3Record> calcWKBJ(List<DistanceRay> degreesList) throws TauPException {
        validateArguments();
        List<SeismicPhase> phaseList = getSeismicPhases();
        List<MSeed3Record> spikeRecords = new ArrayList<>();
        float[][] sourceTerm = effectiveSourceTerm( sourceArgs.getMw(), (float) deltaT,  1000);
        Instant sourceTime = new MSeed3Record().getStartInstant();
        for (DistanceRay distVal : degreesList) {
            double degrees = distVal.getDegrees(getRadiusOfEarth());
            List<Arrival> allArrivals = new ArrayList<>();
            for (SeismicPhase phase : phaseList) {
                List<Arrival> phaseArrivals = distVal.calculate(phase);
                allArrivals.addAll(phaseArrivals);
            }

            Arrival first = Arrival.getEarliestArrival(allArrivals);
            double firstTime = first == null ? 0 : first.getTime();
            Arrival last = Arrival.getLatestArrival(allArrivals);
            double lastTime = last == null ? 0 : last.getTime();
            int startTime = (int) (Math.round(firstTime) - 10);
            startTime = 0;
            double maxTime = lastTime - startTime + 200;
            int numSamples = (int)(Math.ceil(maxTime / getDeltaT())) ;

            float[][] theta_rtz = calcThetaTimeseriesRTZ(degrees, allArrivals, startTime, getDeltaT(), numSamples);
            float[][] rtz = calcThetaTimeseriesRTZ(degrees, allArrivals, startTime, getDeltaT(), numSamples);
            rtz[RAD_IDX] = dumbconvolve(rtz[RAD_IDX], sourceTerm[RAD_IDX]);
            rtz[TRANS_IDX] = dumbconvolve(rtz[TRANS_IDX], sourceTerm[TRANS_IDX]);
            rtz[VERT_IDX] = dumbconvolve(rtz[VERT_IDX], sourceTerm[VERT_IDX]);
            //startTime += (int) (Math.round(sourceTerm[VERT_IDX].length*getDeltaT()));
            String staCode = "S"+Math.round(degrees);
            if (staCode.length()> 8) { staCode = staCode.substring(8);}
            MSeed3EH eh = createEH(degrees, getOriginTime(), allArrivals);
            eh.setTimeseriesUnit("m");
            spikeRecords.addAll(packageMSeed3(rtz[VERT_IDX], rtz[RAD_IDX], rtz[TRANS_IDX], staCode, startTime));
            spikeRecords.add(packageMSeed3(theta_rtz[VERT_IDX], staCode, "THETA", "Z", startTime));
            for(MSeed3Record msr: spikeRecords) {
                msr.setExtraHeaders(eh.getEH());
                msr.setStartDateTime(getOriginTime());
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
        int i;
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
        float Mo = (float) MomentMagnitude.mw_to_N_m(momentMag);

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
        if (allArrivals.isEmpty()) {
            return rtz;
        }
        // so have at least one arrival
        double radiusEarthMeter = allArrivals.get(0).getTauModel().getRadiusOfEarth()*1000;
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
                            double psvAmpFactor = thetaArrival.getEnergyFluxFactorReflTransPSV();
                            double incidentAngle = thetaArrival.getIncidentAngleDegree();
                            double transverseAmpFactor = thetaArrival.getEnergyFluxFactorReflTransSH();
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

    DataOutputStream writer;

    public DataOutputStream getOutputStream() throws IOException {
        if (writer == null) {
            if(!outputTypeArgs.getOutFile().equals("stdout")) {
                writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputTypeArgs.getOutFile())));
            } else {
                writer = new DataOutputStream(new BufferedOutputStream(System.out));
            }
        }
        return writer;
    }

    @Override
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        for (SeismicPhase phase : phaseList) {
            List<Arrival> phaseArrivals = new ArrayList<>();
            for (RayCalculateable shoot : shootables) {
                phaseArrivals.addAll(shoot.calculate(phase));
            }
            if (!phaseArrivals.isEmpty()) {
                arrivals.add(phaseArrivals.get(0));
            }
        }
        return Arrival.sortArrivals(arrivals);
    }

    @Override
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException {

    }

    @Override
    public void destroy() throws TauPException {

    }


    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    public List<DistanceRay> getDistances() throws TauPException {
        return distanceArgs.getDistances();
    }

    public ZonedDateTime getOriginTime() {
        if (origintime == null ) {
            return defaultOriginTime;
        }
        return origintime;
    }

    ZonedDateTime defaultOriginTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));


    @CommandLine.Mixin
    SeismicSourceArgs sourceArgs = new SeismicSourceArgs();

    @CommandLine.Option(names = "--otime", description = "event origin time, as ISO8601")
    ZonedDateTime origintime;

    @CommandLine.Mixin
    SeismogramOutputTypeArgs outputTypeArgs;
}
