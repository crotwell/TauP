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
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import edu.sc.seis.seisFile.fdsnws.quakeml.*;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.seisFile.fdsnws.stationxml.Network;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed3.*;
import edu.sc.seis.seisFile.mseed3.ehbag.Marker;
import edu.sc.seis.seisFile.mseed3.ehbag.Path;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;
import static edu.sc.seis.TauP.cmdline.args.OutputTypes.MS3;
import static edu.sc.seis.TauP.cmdline.args.OutputTypes.SAC;

@CommandLine.Command(name = "spikes",
        description = "Calculate spike seismograms",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_Spikes extends TauP_AbstractPhaseTool {

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
        return 1.0/sps;
    }

    /**
     * Set the value of deltaT.
     *
     * @param v Value to assign to deltaT.
     */
    public void setDeltaT(double v) {
        this.sps = 1/v;
    }

    @Override
    public void validateArguments() throws TauPException {
        if (!(getOutputFormat().equals(MS3) || getOutputFormat().equals(SAC))) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Unsupported Output Format: " + getOutputFormat());
        }
        if (modelArgs.getSourceDepths().size() > 1) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Multiple source depths unsupported: " + modelArgs.getSourceDepths().size());
        }
        sourceArgs.validateArguments();
        sourceArgs.validateArgumentsForAmplitude(modelArgs, getRayCalculatables());
    }

    @Override
    public void init() throws TauPException {
        super.init();
    }

    @Override
    public void start() throws IOException, TauPException {
        validateArguments();
        try {
            List<RayCalculateable> rays = getRayCalculatables();
            List<MSeed3Record> allRecords = new ArrayList<>();
            List<MSeed3Record> spikeRecords = calcSpikes(rays);
            allRecords.addAll(spikeRecords);

            if (outputTypeArgs.isMS3()) {
                setOutFileBase("taup_spikes");
                DataOutputStream dos = getOutputStream();
                for (MSeed3Record ms3 : allRecords) {
                    dos.write(ms3.asByteBuffer().array());
                }
                dos.close();
            } else if (outputTypeArgs.isSAC()) {
                try {
                    for (MSeed3Record ms3r : allRecords) {
                        SacTimeSeries sac = MSeed3Convert.convert3ToSac(ms3r);
                        setOutFileBase("taup_spikes_" + sac.getHeader().getGcarc()+"_"+sac.getHeader().getKcmpnm());
                        DataOutputStream dos = getOutputStream();
                        sac.write(dos);
                        dos.close();
                        this.writer = null;
                    }
                } catch (SeedFormatException | edu.iris.dmc.seedcodec.CodecException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new TauPException("Unknown output type: "+getOutputFormat());
            }
        } catch (IOException e) {
            throw new TauPException(e);
        }
    }

    public List<MSeed3Record> calcSpikes(List<RayCalculateable> degreesList) throws TauPException {

        // testing....
        //setDeltaT(.10);
        List<MSeed3Record> spikeRecords = new ArrayList<>();

        for (double sourceDepth : getSourceDepths()) {
            for (double receiverDepth : getReceiverDepths()) {
                List<SeismicPhase> phaseList = calcSeismicPhases(sourceDepth,
                        List.of(receiverDepth), modelArgs.getScatterer());
                spikeRecords.addAll(calcSpikes(degreesList, phaseList));
            }
        }
        return spikeRecords;
    }
    public List<MSeed3Record> calcSpikes(List<RayCalculateable> rayList, List<SeismicPhase> phaseList) throws TauPException {

        List<MSeed3Record> spikeRecords = new ArrayList<>();
        List<DistanceRay> degreesList = new ArrayList<>();
        Set<Double> distFromArrival = new HashSet<>();
        for (RayCalculateable ray : rayList) {
            if (!ray.hasSeismicSource()) {
                ray.setSeismicSource(sourceArgs.getSeismicSource());
            }
            if (ray instanceof DistanceRay) {
                degreesList.add((DistanceRay)ray);
            } else {
                // have to get Arrivals to find distance to then redo calc
                List<Arrival> rayArrivals = new ArrayList<>();
                for (SeismicPhase phase : phaseList) {
                    if (TauP_Time.isRayOkForPhase(ray, phase)) {
                        rayArrivals.addAll(ray.calculate(phase));
                    }
                }
                for (Arrival arr : rayArrivals) {
                    distFromArrival.add(arr.getDist());
                }
            }
        }
        for (Double radian : distFromArrival) {
            DistanceRay dr = DistanceRay.ofRadians(radian);
            if (!dr.hasAzimuth() && latLonArgs.hasAzimuth()) {
                dr.setAzimuth(latLonArgs.getAzimuth());
            }
            dr.setSeismicSource(sourceArgs.getSeismicSource());
            degreesList.add(dr);
        }
        for (DistanceRay dr : degreesList) {
            List<Arrival> allArrivals = new ArrayList<>();
            List<MSeed3Record> componentRecords = new ArrayList<>();
            double degrees = dr.getDegrees(getRadiusOfEarth());
            for (SeismicPhase phase : phaseList) {
                List<Arrival> phaseArrivals = dr.calculate(phase);
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
                int width = (int) Math.round(pulseWidth*sps) + 1;
                if (width < 2) {width=2;}

                float t = (float) shAmpFactor;
                float r = (float) (psvAmpFactor * Math.cos(incidentAngle*Math.PI/180));
                float v = (float) (psvAmpFactor * Math.sin(incidentAngle*Math.PI/180));
                for (int w = 0; w < width; w++) {
                    transverse[timeIdx+w] += t;
                    radial[timeIdx+w] += r;
                    vertical[timeIdx+w] += v;
                }
            }

            String staCode = null;
            if (dr.hasReceiver()) {
                if ( dr.getReceiver() instanceof Station) {
                    Station sta = (Station) dr.getReceiver();
                    staCode = sta.getStationCode();
                } else if (dr.getReceiver() instanceof Channel) {
                    Channel chan = (Channel) dr.getReceiver();
                    staCode = chan.getStationCode();
                }
            }
            if (staCode == null ) {
                staCode = "S"+degrees;
                staCode = staCode.replace(".", "");
                if (staCode.length()> 8) { staCode = staCode.substring(8);}
            }
            componentRecords.add(packageMSeed3(vertical, staCode, "SP", "Z", startTime ));
            componentRecords.add(packageMSeed3(radial, staCode, "SP", "R", startTime ));
            componentRecords.add(packageMSeed3(transverse, staCode, "SP", "T", startTime ));
            for (MSeed3Record ms3 : componentRecords) {
                MSeed3EH eh = createEH(dr, getOriginTime(), allArrivals, ms3.getSourceId());
                ms3.setStartDateTime(getOriginTime());
                ms3.setExtraHeaders(eh.getEH());
            }
            spikeRecords.addAll(componentRecords);
        }
        return spikeRecords;
    }

    public MSeed3EH createEH(DistanceRay dr, ZonedDateTime startDateTime, List<Arrival> allArrivals, FDSNSourceId sourceId) throws TauPException {
        MSeed3EH eh = new MSeed3EH();

        // assume single source
        double sourceDepth = modelArgs.getSourceDepths().isEmpty() ? 0 : modelArgs.getSourceDepths().get(0);
        Float deg = (float) dr.getDegrees(getRadiusOfEarth());
        Float az = dr.hasAzimuth() ? dr.getAzimuth().floatValue() : null;
        Float baz = dr.hasBackAzimuth() ? dr.getBackAzimuth().floatValue() : null;

        Origin origin = new Origin();
        origin.setDepth(new RealQuantity((float) sourceDepth));
        origin.setTime(new Time(startDateTime.toInstant()));

        float olat = 0;
        float olon = 0;
        if (dr.hasSource()) {
            Location loc = dr.getSource().asLocation();
            olat = (float) loc.getLatitude();
            olon = (float) loc.getLongitude();
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
        Marker oMarker = new Marker("origin", startDateTime, MSeed3EHKeys.MARKER_MODELED, "");
        eh.addToBag(oMarker);

        // only add chan if we can get lat,lon
        Station sta = new Station(new Network(sourceId.getNetworkCode()), sourceId.getStationCode());
        if (dr.hasReceiver()) {
            sta.setLatitude((float) dr.getReceiver().asLocation().getLatitude());
            sta.setLongitude((float) dr.getReceiver().asLocation().getLongitude());
        } else if (dr.hasAzimuth()  && !dr.isGeodetic()) {
            sta.setLatitude((float) SphericalCoords.latFor(origin.asLocation(), dr.getDegrees(getRadiusOfEarth()), dr.getAzimuth()));
            sta.setLongitude((float) SphericalCoords.lonFor(origin.asLocation(), dr.getDegrees(getRadiusOfEarth()), dr.getAzimuth()));
        } else {
            //System.err.println("cannot calc station loc: rec: "+dr.hasReceiver()+" az: "+dr.hasAzimuth()+" geod: "+dr.isGeodetic());
        }
        Channel chan = new Channel(sta, sourceId.getLocationCode(), sourceId.getChannelCode());
        chan.setSourceId(sourceId.toString());
        if (dr.hasReceiver() && dr.getReceiver().asLocation().hasDepth()) {
            chan.setDepth(dr.getReceiver().asLocation().getDepthMeter().floatValue());
            chan.setElevation(-1 * dr.getReceiver().asLocation().getDepthMeter().floatValue());
        } else {
            chan.setDepth(0);
            chan.setElevation(0);
        }

        if (sta.getLatitude() != null && sta.getLongitude()!= null) {
            // only if sta has loc
            if (origin != null && chan.asLocation() != null) {
                if (az == null && !dr.isGeodetic()) {
                    az = (float) SphericalCoords.azimuth(origin.asLocation(), chan.asLocation());
                }
                if (baz == null && !dr.isGeodetic()) {
                    baz = (float) SphericalCoords.azimuth(chan.asLocation(), origin.asLocation());
                }
            }
        }
        Path path = new Path(deg, az, baz);
        eh.addToBag(path);

        if (sourceId.getSubsourceCode().charAt(0) == 'Z') {
            chan.setAzimuth(0);
            chan.setDip(-90);
        } else if (sourceId.getSubsourceCode().charAt(0) == 'R') {
            if (baz != null) {
                chan.setAzimuth((baz + 180) % 360);
            }
            chan.setDip(0);
        } else if (sourceId.getSubsourceCode().charAt(0) == 'T') {
            if (baz != null) {
                chan.setAzimuth((baz + 180 + 90) % 360);
            }
            chan.setDip(0);
        }
        eh.addToBag(chan);
        eh.setTimeseriesUnit("m");
        JSONObject bagEh = eh.getBagEH();
        if (! bagEh.has(MSeed3EHKeys.Y)) {
            bagEh.put(MSeed3EHKeys.Y, new JSONObject());
        }
        JSONObject y = bagEh.getJSONObject(MSeed3EHKeys.Y);
        y.put(MSeed3EHKeys.PROC, "synth");
        if (! y.has(MSeed3EHKeys.REQUEST)) {
            y.put(MSeed3EHKeys.REQUEST, new JSONObject());
        }
        JSONObject req = y.getJSONObject(MSeed3EHKeys.REQUEST);
        req.put(MSeed3EHKeys.DATACENTER, "TauP"+ BuildVersion.getVersion());
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
        float[][] sourceTerm = effectiveSourceTerm( sourceArgs.getMw(), (float)( 1/sps),  1000);
        Instant sourceTime = new MSeed3Record().getStartInstant();
        for (DistanceRay dr : degreesList) {
            double degrees = dr.getDegrees(getRadiusOfEarth());
            List<Arrival> allArrivals = new ArrayList<>();
            for (SeismicPhase phase : phaseList) {
                List<Arrival> phaseArrivals = dr.calculate(phase);
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
            spikeRecords.addAll(packageMSeed3(rtz[VERT_IDX], rtz[RAD_IDX], rtz[TRANS_IDX], staCode, startTime));
            spikeRecords.add(packageMSeed3(theta_rtz[VERT_IDX], staCode, "THETA", "Z", startTime));
            for(MSeed3Record msr: spikeRecords) {
                MSeed3EH eh = createEH(dr, getOriginTime(), allArrivals, msr.getSourceId());
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
        Arrival.sortArrivals(arrivals);
        return arrivals;
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

    public List<RayCalculateable> getRayCalculatables() {
        List<RayCalculateable> out = distanceArgs.getRayCalculatables(sourceArgs);
        if (latLonArgs.hasAzimuth()) {
            for (RayCalculateable rc : out) {
                if (!rc.hasAzimuth()) {
                    rc.setAzimuth(latLonArgs.getAzimuth());
                }
            }
        }
        return out;
    }

    public ZonedDateTime getOriginTime() {
        if (origintime == null ) {
            return defaultOriginTime;
        }
        return origintime;
    }

    public DistanceLengthArgs getDistanceLengthArgs() {
        return this.distanceArgs;
    }

    public boolean hasEventLatLon() {
        return  latLonArgs.hasEventLatLon() || qmlStaxmlArgs.hasQml();
    }

    public boolean hasStationLatLon() {
        return latLonArgs.hasStationLatLon() || qmlStaxmlArgs.hasStationXML();
    }

    public List<LatLonLocatable> getStationLatLon() throws TauPException {
        List<LatLonLocatable> staList = new ArrayList<>();
        staList.addAll(latLonArgs.getStationLocations());
        staList.addAll(qmlStaxmlArgs.getStationLocations());
        return staList;
    }

    public List<LatLonLocatable> getEventLatLon() throws TauPException {
        List<LatLonLocatable> eventLocs = new ArrayList<>();
        eventLocs.addAll(latLonArgs.getEventLocations());
        eventLocs.addAll(qmlStaxmlArgs.getEventLocations());
        return eventLocs;
    }

    ZonedDateTime defaultOriginTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));


    @CommandLine.Mixin
    SeismicSourceArgs sourceArgs = new SeismicSourceArgs();

    @CommandLine.Option(names = "--otime", description = "event origin time, as ISO8601 yyyy-MM-ddTHH:mm:ss.SZ, append Z for UTC times")
    ZonedDateTime origintime;

    @CommandLine.Mixin
    SeismogramOutputTypeArgs outputTypeArgs;


    /**
     * deltaT of the seismogram, default is .05 which gives 20 sps.
     */
    @CommandLine.Option(names = "sps",
            defaultValue = "20",
            description = "Samples per second for the output seismogram, defaults to $DEFAULT_VALUE"
    )
    protected double sps = 20;

    @CommandLine.Option(names = "--pulsewidth",
            defaultValue = "1.0",
            description = "Width in seconds of the spike pulse for each arrival")
    double pulseWidth = 1.0;

    @CommandLine.Mixin
    DistanceLengthArgs distanceArgs = new DistanceLengthArgs();


    @CommandLine.ArgGroup(validate = false, heading = "Lat,Lon influenced by:%n")
    LatLonAzBazArgs latLonArgs = new LatLonAzBazArgs();

    @CommandLine.Mixin
    GeodeticArgs geodeticArgs = new GeodeticArgs();

    @CommandLine.Mixin
    QmlStaxmlArgs qmlStaxmlArgs = new QmlStaxmlArgs();
}
