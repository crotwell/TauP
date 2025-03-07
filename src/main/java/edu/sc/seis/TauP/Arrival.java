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

import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.TWOPI;

/**
 * convenience class for storing the parameters associated with a phase arrival.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 * 
 */
public class Arrival {

    public Arrival(SeismicPhase phase,
                   double time,
                   double dist,
                   double rayParam,
                   int rayParamIndex,
                   double dRPdDist) {
        this(phase,
                time,
                dist,
                rayParam,
                rayParamIndex,
                DistanceRay.ofRadians(dist),
                phase.getName(),
                phase.getPuristName(),
                phase.getSourceDepth(),
                phase.getReceiverDepth(),
                phase.calcTakeoffAngle(rayParam),
                phase.calcIncidentAngle(rayParam),
                dRPdDist);
    }
    public Arrival(SeismicPhase phase,
                   double time,
                   double dist,
                   double rayParam,
                   int rayParamIndex,
                   RayCalculateable searchDist,
                   String name,
                   String puristName,
                   double sourceDepth,
                   double receiverDepth,
                   double takeoffAngleRadian,
                   double incidentAngleRadian,
                   double dRPdDist) {

        if (Double.isNaN(time)) {
            throw new IllegalArgumentException("Time cannot be NaN");
        }
        if (rayParamIndex < 0) {
            throw new IllegalArgumentException("rayParamIndex cannot be negative: "+rayParamIndex);
        }
        this.phase = phase;
        this.time = time;
        this.dist = dist;
        this.rayParam = rayParam;
        this.rayParamIndex = rayParamIndex;
        this.searchCalc = searchDist;
        this.name = name;
        this.puristName = puristName;
        this.sourceDepth = sourceDepth;
        this.receiverDepth = receiverDepth;
        this.takeoffAngleRadian = takeoffAngleRadian;
        this.incidentAngleRadian = incidentAngleRadian;
        this.dRPdDist = dRPdDist;
    }


    /** phase that generated this arrival. */
    private final SeismicPhase phase;

    /** travel time in seconds */
    private final double time;

    /** angular distance (great circle) in radians */
    private final double dist;

    /** ray parameter in seconds per radians. */
    private final double rayParam;

    private final int rayParamIndex;

    private final double dRPdDist;

    /** original angular search criteria, usually distance (great circle) in degrees.
     * May differ from dist by multiple of 180
     * or be 180 - dist for long way around. */
    private RayCalculateable searchCalc;

    /** phase name */
    private final String name;

    /** phase name changed for true depths */
    private final String puristName;

    /** source depth in kilometers */
    private final double sourceDepth;

    /** receiver depth in kilometers */
    private final double receiverDepth;

    /** pierce and path points */
    private TimeDist[] pierce;

    private List<ArrivalPathSegment> pathSegments;

    private final double incidentAngleRadian;
    
    private final double takeoffAngleRadian;

    private Arrival relativeToArrival = null;

    public static List<Arrival> sortArrivals(List<Arrival> arrivals) {
        arrivals.sort(Comparator.comparingDouble(Arrival::getTime));
        return arrivals;
    }

    public static List<Arrival> onlyFirst(List<Arrival> arrivalList) {
        List<Arrival> first = new ArrayList<>();
        List<Arrival> copyList = new ArrayList<>(arrivalList);
        copyList = Arrival.sortArrivals(copyList);
        while (!copyList.isEmpty()) {
            Arrival early = copyList.get(0);
            first.add(early);
            copyList.remove(early);
            List<Arrival> samePhase = new ArrayList<>();
            for (Arrival a : copyList) {
                if (a.getPhase() == early.getPhase()) {
                    samePhase.add(a);
                }
            }
            copyList.removeAll(samePhase);
        }
        return first;
    }

    public  String getCommentLine() {
        String outName = getName();
        if ( ! getName().equals(getPuristName())) {
            outName+="("+getPuristName()+")";
        }
        String out = outName + " at "
                + Outputs.formatTime(getTime())
                + " seconds at "
                + Outputs.formatDistance(getDistDeg())
                + " degrees for a "
                + Outputs.formatDepth(getSourceDepth())
                + " km deep source in the " + getPhase().getTauModel().getModelName() + " model with rayParam "
                + Outputs.formatRayParam(Math.PI / 180 * getRayParam())
                + " s/deg";
        if (getPhase().getReceiverDepth() != 0.0) {
            out += ", receiver at depth: "+getPhase().getReceiverDepth()+" km";
        }
        if (getRayCalculateable().hasDescription()) {
            out += ", "+getRayCalculateable().getDescription()+".";
        } else {
            out += ".";
        }
        return out;
    }

    // get set methods
    /** @return the phase used to calculate this arrival. */
    public SeismicPhase getPhase() {
        return phase;
    }

    /** @return travel time in seconds */
    public double getTime() {
        return time;
    }
    
    /**@return travel time as a Duration */
    public Duration getDuration() {
        return Duration.ofNanos(Math.round(getTime()*1000000000));
    }

    /** returns travel distance in radians */
    public double getDist() {
        return dist;
    }

    /**
     * returns travel distance in degrees.
     */
    public double getDistDeg() {
        return SphericalCoords.RtoD * getDist();
    }

    /**
     * returns distance in radians and in the range 0-PI. Note this may not be
     * the actual distance traveled.
     */
    public double getModuloDist() {
        double moduloDist = getDist() % TWOPI;
        if(moduloDist > Math.PI) {
            moduloDist = TWOPI - moduloDist;
        }
        return moduloDist;
    }

    /**
     * returns distance in degrees and in the range 0-180. Note this may not be
     * the actual distance traveled.
     */
    public double getModuloDistDeg() {
        return SeismicPhase.distanceTrim180(getDistDeg());
    }

    public void setSearchValue(RayCalculateable searchVal) {
        this.searchCalc = searchVal;
    }

    /**
     * returns search distance in degrees.
     */
    public double getSearchDistDeg() {
        if (this.searchCalc instanceof DistanceRay) {
            return ((DistanceRay)this.searchCalc).getDegrees(getPhase().getTauModel().getRadiusOfEarth());
        }
        return this.getDistDeg();
    }

    /**
     * returns search distance in degrees and in the range 0-180. Note this may not be
     * the actual distance traveled.
     */
    public double getModuloSearchDistDeg() {
        return SeismicPhase.distanceTrim180(getSearchDistDeg());
    }

    public static final double MANY_LAPS_PLUS_180 = 360*100+180;
    public boolean isLongWayAround() {
        double shortWay = ((MANY_LAPS_PLUS_180 + getSearchDistDeg() - getDistDeg()) % 360 ) -180;
        double longWay = ((MANY_LAPS_PLUS_180 + getSearchDistDeg() - (360-getDistDeg())) % 360) -180;
        return Math.abs(longWay) < Math.abs(shortWay);
    }

    /** returns ray parameter in seconds per radian */
    public double getRayParam() {
        return rayParam;
    }

    /** returns ray parameter in seconds per deg */
    public double getRayParamDeg() {
        return getRayParam()/ SphericalCoords.RtoD;
    }

    public double getDRayParamDDelta() {
        return dRPdDist;
    }

    public double getDRayParamDDeltaDeg() {
        return dRPdDist/ SphericalCoords.RtoD/ SphericalCoords.RtoD;
    }


    /**
     * Geometrical spreading factor for amplitude, sqrt of energy spreading.
     * See Fundamentals of Modern Global Seismology, ch 13, eq 13.19.
     *
     * @throws TauModelException
     */
    public double getAmplitudeGeometricSpreadingFactor() throws TauModelException {
        double d = getEnergyGeometricSpreadingFactor();
        if (d < 0) throw new RuntimeException("energy geo spread is neg "+getDistDeg()+" "+d);
        return Math.sqrt(getEnergyGeometricSpreadingFactor());
    }


    /**
     * Energy Geometrical spreading factor.
     * See Fundamentals of Modern Global Seismology, ch 13, eq 13.10.
     * Note that eq 13.10 has divide by zero in case of a horizontal ray arriving at the receiver.
     *
     */
    public double getEnergyGeometricSpreadingFactor() {
        double out = 1;
        TauModel tMod = getPhase().getTauModel();
        double R = tMod.radiusOfEarth;
        out *= getPhase().velocityAtSource()/
                ((R-getReceiverDepth())*(R-getReceiverDepth())*(R-getSourceDepth()));
        double takeoffRadian = getTakeoffAngleRadian();
        if ( ! getPhase().getInitialPhaseSegment().isDownGoing) {
            takeoffRadian = Math.PI-takeoffRadian;
        }
        out *= Math.tan(takeoffRadian)/Math.cos(getIncidentAngleRadian());
        out *= 1/Math.sin(getModuloDist());
        double dRPdDist = getDRayParamDDelta(); // dp/ddelta = dT/ddelta
        out *= Math.abs(dRPdDist);
        return out;
    }

    /**
     * Calculates the product of the reflection and transmission coefficients for all discontinuities along the path
     * of this arrival in the P-SV plane. Note that this may not give accurate results for certain wave types,
     * such as head or diffracted waves.
     * @return
     * @throws VelocityModelException
     * @throws SlownessModelException
     */
    public double getEnergyFluxFactorReflTransPSV() throws VelocityModelException, SlownessModelException {
        return getPhase().calcEnergyFluxFactorReflTranPSV(this);
    }

    /**
     * Calculates the product of the reflection and transmission coefficients for all discontinuities along the path
     * of this arrival for transverse, SH, waves. If any segment on the path is a P wave, the result will be zero.
     * Note that this may not give accurate results for certain wave types,
     * such as head or diffracted waves.
     * @return
     * @throws VelocityModelException
     * @throws SlownessModelException
     */
    public double getEnergyFluxFactorReflTransSH() throws VelocityModelException, SlownessModelException {
        return getPhase().calcEnergyFluxFactorReflTranSH(this);
    }

    /**
     * Calculates the amplitude factor, 1/sqrt(density*vel) times reflection/tranmission coefficient times geometric spreading, for the
     * arrival. Note this is only an approximation of amplitude as the source radiation magnitude and pattern is
     * not included, and this may not give accurate results for certain wave types, such as head or diffracted waves.
     * <p>
     * See FMGS eq 17.74
     */
    public double getAmplitudeFactorPSV() throws TauModelException, SlownessModelException {
        SeismicSourceArgs sourceArgs = getRayCalculateable().getSourceArgs();
        if (sourceArgs == null) {
            throw new TauModelException("sourceArgs is null, RayCalc:"+getRayCalculateable());
        }

        double ampFactor = getAmplitudeFactorPSV(sourceArgs.getMoment(), sourceArgs.getAttenuationFrequency(), sourceArgs.getNumFrequencies());
        if (sourceArgs.hasStrikeDipRake() && searchCalc.azimuth != null ) {
            double[] radiationPattern = sourceArgs.calcRadiationPat( searchCalc.azimuth, getTakeoffAngleDegree());
            double radTerm = 1;
            if (getPhase().getPhaseSegments().get(0).isPWave) {
                radTerm = radiationPattern[0];
            } else {
                radTerm = radiationPattern[1];
            }
            ampFactor *= radTerm;
        } else if (sourceArgs.getStrikeDipRake() != null && searchCalc.azimuth == null ) {
            // change to TauPException
            throw new TauModelException("Amplitude with Strike-dip-rake requires azimuth");
        }
        return ampFactor;
    }

    public double getAmplitudeFactorPSV(double momentRate, double attenuationFrequency, int numFreq)
            throws TauModelException, SlownessModelException {
        // dimensionaless
        double refltran = getEnergyFluxFactorReflTransPSV();
        double freeFactor = 1.0;
        if (getReceiverDepth() <= 1.0) {
            VelocityModel vMod = getPhase().getTauModel().getVelocityModel();
            VelocityLayer top = vMod.getVelocityLayer(0);
            Complex[] freeSurfRF;
            ReflTransFreeSurface rtFree = ReflTransFreeSurface.createReflTransFreeSurface(top.getTopPVelocity(), top.getTopSVelocity(), top.getTopDensity());
            if (getPhase().getFinalPhaseSegment().isPWave) {
                freeSurfRF = rtFree.getFreeSurfaceReceiverFunP(getRayParam() / vMod.getRadiusOfEarth());
            } else {
                freeSurfRF = rtFree.getFreeSurfaceReceiverFunSv(getRayParam() / vMod.getRadiusOfEarth());
            }
            freeFactor = Complex.abs(Complex.sqrt(freeSurfRF[0].times(freeSurfRF[0].plus(freeSurfRF[1].times(freeSurfRF[1])))));
        }
        double geoSpread = getAmplitudeGeometricSpreadingFactor(); // 1/km
        //       km/s
        double sourceVel = getPhase().velocityAtSource();
        //                                           Mg/m3 * (km/s)3 => 1e3 Kg/m3 * 1e9 (m3/s3) => 1e12 Kg /s3
        double radiationTerm = 4*Math.PI*getPhase().densityAtSource()*sourceVel*sourceVel*sourceVel*1e12;
        double attenuation=1;
        if (attenuationFrequency > 0) {
            attenuation = calcAttenuation(attenuationFrequency, numFreq);
        }
        return 1                   // units:
                * attenuation      // 1
                * freeFactor       // 1
                * momentRate       // Kg m2 / s3
                * refltran         // 1
                * geoSpread        // 1/km
                / radiationTerm    // 1/(Kg/s3)
                / 1e3;             //  m2/km =>  m / 1e3
    }

        /**
         * Calculates the amplitude factor, 1/sqrt(density*vel) times reflection/tranmission coefficient times geometric spreading, for the
         * arrival. Note this is only an approximation of amplitude as the source radiation magnitude and pattern is
         * not included, and this may not give accurate results for certain wave types, such as head or diffracted waves.
         * @return
         * @throws TauModelException
         * @throws VelocityModelException
         * @throws SlownessModelException
         */
    public double getAmplitudeFactorSH() throws TauModelException, VelocityModelException, SlownessModelException {
        SeismicSourceArgs sourceArgs = getRayCalculateable().getSourceArgs();
        double ampFactor = getAmplitudeFactorSH(sourceArgs.getMoment(), sourceArgs.getAttenuationFrequency(), sourceArgs.getNumFrequencies());
        if (sourceArgs.hasStrikeDipRake() && searchCalc.azimuth != null ) {
            double[] radiationPattern = sourceArgs.calcRadiationPat( searchCalc.azimuth, getTakeoffAngleDegree());
            ampFactor *= radiationPattern[2];
        } else if (sourceArgs.hasStrikeDipRake() && searchCalc.azimuth == null ) {
            // change to TauPException
            throw new TauModelException("Amplitude with Strike-dip-rake requires azimuth");
        }
        return ampFactor;
    }

    public double getAmplitudeFactorSH(double momentRate, double attenuationFrequency, int numFreq)
            throws TauModelException, SlownessModelException {
        double refltran = getEnergyFluxFactorReflTransSH();
        // avoid NaN in case of no S wave legs where geo spread returns INFINITY
        if (refltran == 0.0) {return 0.0;}
        double geoSpread = getAmplitudeGeometricSpreadingFactor();
        double sourceVel = getPhase().velocityAtSource();
        double radiationTerm = 4*Math.PI*getPhase().densityAtSource()*sourceVel*sourceVel*sourceVel*1e12;
        double attenuation=1;
        if (attenuationFrequency > 0) {
            attenuation = calcAttenuation(attenuationFrequency, numFreq);
        }
        double freeFactor =  1.0;
        if (getReceiverDepth() <= 1.0) {
            // should be exactly 2.0, but go through the steps so looks like PSv case
            VelocityModel vMod = getPhase().getTauModel().getVelocityModel();
            VelocityLayer top = vMod.getVelocityLayer(0);
            ReflTransFreeSurface rtFree = ReflTransFreeSurface.createReflTransFreeSurface(top.getTopPVelocity(), top.getTopSVelocity(), top.getTopDensity());

            freeFactor = rtFree.getFreeSurfaceReceiverFunSh(getRayParam() / vMod.getRadiusOfEarth());

            //freeFactor = Complex.abs(Complex.sqrt(freeSurfRF[0].times(freeSurfRF[0].plus(freeSurfRF[1].times(freeSurfRF[1])))));
        }

        return 1                   // units:
                * attenuation      // 1
                * freeFactor       // 1
                * momentRate       // Kg m2 / s3
                * refltran         // 1
                * geoSpread        // 1/km
                / radiationTerm    // 1/(Kg/s3)
                / 1e3;             //  m2/km =>  m / 1e3
    }

    public double getRadiationPatternPSV() {
        SeismicSourceArgs sourceArgs = getRayCalculateable().getSourceArgs();
        double radTerm = 1;
        if (sourceArgs != null && sourceArgs.hasStrikeDipRake() && searchCalc.azimuth != null ) {
            double[] radiationPattern = sourceArgs.calcRadiationPat( searchCalc.azimuth, getTakeoffAngleDegree());
            if (getPhase().getPhaseSegments().get(0).isPWave) {
                radTerm = radiationPattern[0];
            } else {
                radTerm = radiationPattern[1];
            }
        }
        return radTerm;
    }

    public double getRadiationPatternSH() {
        SeismicSourceArgs sourceArgs = getRayCalculateable().getSourceArgs();
        double radTerm = 1;
        if (sourceArgs != null && sourceArgs.hasStrikeDipRake() && searchCalc.azimuth != null ) {
            double[] radiationPattern = sourceArgs.calcRadiationPat( searchCalc.azimuth, getTakeoffAngleDegree());
            if (getPhase().getPhaseSegments().get(0).isPWave) {
                radTerm = 0;
            } else {
                radTerm = radiationPattern[2];
            }
        }
        return radTerm;
    }

    public double getIncidentAngleDegree() {
        return getIncidentAngleRadian()* SphericalCoords.RtoD;
    }

    public double getIncidentAngleRadian() {
        return incidentAngleRadian;
    }

    public double getTakeoffAngleDegree() {
        return getTakeoffAngleRadian()* SphericalCoords.RtoD;
    }
    
    public double getTakeoffAngleRadian() {
        return takeoffAngleRadian;
    }

    public double velocityAtSource() {
        return getPhase().velocityAtSource();
    }

    public double radialSlownessAtSource() {
        double srcVel = velocityAtSource();
        double rofE = getPhase().getTauModel().getRadiusOfEarth();
        double srcRadius = rofE - getSourceDepth();
        double radSlow = Math.sqrt(1/(srcVel*srcVel) - getRayParam()*getRayParam()/(srcRadius*srcRadius));
        if (! Double.isFinite(radSlow)
                && Math.abs( 1/(srcVel*srcVel) - getRayParam()*getRayParam()/(srcRadius*srcRadius)) < 1e-6) {
            // due to interpolation/rounding horizontal ray might give NaN, if close to zero but negative,
            // just return zero
            radSlow = 0;
        }
        return radSlow;
    }


    public double velocityAtReceiver() {
        return getPhase().velocityAtReceiver();
    }

    public double radialSlownessAtReceiver() {
        double recVel = velocityAtReceiver();
        double rofE = getPhase().getTauModel().getRadiusOfEarth();
        double recRadius = rofE - getReceiverDepth();
        return Math.sqrt(1/(recVel*recVel) - getRayParam()*getRayParam()/(recRadius*recRadius));
    }

    public int getRayParamIndex() {
        return rayParamIndex;
    }

    /** returns phase name */
    public String getName() {
        return name;
    }

    /**
     * returns purist's version of name. Depths are changed to reflect the true
     * depth of the interface.
     */
    public String getPuristName() {
        return puristName;
    }

    /** returns source depth in kilometers */
    public double getSourceDepth() {
        return sourceDepth;
    }

    /** returns receiver (station) depth in kilometers */
    public double getReceiverDepth() {
        return receiverDepth;
    }


    /** returns shallowest point on path, in kilometers */
    public TimeDist getShallowestPierce() {
        TimeDist[] pierce = getPierce();
        TimeDist shallowest = pierce[0];
        for (TimeDist td : pierce) {
            if (td.getDepth() < shallowest.getDepth()) {
                shallowest = td;
            }
        }
        return shallowest;
    }

    /** returns deepest point on path, in kilometers */
    public TimeDist getDeepestPierce() {
        TimeDist[] pierce = getPierce();
        TimeDist deepest = pierce[0];
        for (TimeDist td : pierce) {
            if (td.getDepth() > deepest.getDepth()) {
                deepest = td;
            }
        }
        return deepest;
    }

    /** returns furthest distance point module pi/180 on path, in radians */
    public TimeDist getFurthestPierce() {
        TimeDist[] pierce = getPierce();
        TimeDist furthest = pierce[0];
        for (TimeDist td : pierce) {
            if (td.getDistRadian() > furthest.getDistRadian()) {
                furthest = td;
            }
        }
        return furthest;
    }

    /** returns pierce points as TimeDist objects. */
    public TimeDist[] getPierce() {
        if (pierce == null) {
            this.pierce = getPhase().calcPierceTimeDist(this).toArray(new TimeDist[0]);
        }
        return pierce;
    }

    /**
     * Calculate attenuation over path at the default frequency. See eq B13.2.2 in FMGS, p374.
     */
    public double calcAttenuation() {
        return calcAttenuation(getRayCalculateable().getSourceArgs().getAttenuationFrequency(),
                getRayCalculateable().getSourceArgs().getNumFrequencies());
    }

    /**
     * Calculate attenuation over path at the given frequency. See eq B13.2.2 in FMGS, p374.
     * Averages over N+1 frequencies from 0 to maxfreq.
     */
    public double calcAttenuation(double maxfreq, int N) {
        double tstar = calcTStar();
        double atten = 0;
        if (Double.isFinite(tstar)) {
            if (N == 0 || N == 1) {
                // only do maxfreq
                atten = Math.pow(Math.E, -1 * Math.PI * maxfreq * tstar);
            } else {
                double deltaFreq = maxfreq/(N-1);
                for (int n = 0; n <= N; n++) {
                    double freq = n*deltaFreq;
                    // attenuation is dispirsive, maybe phase shift a cosine by XXX
                    atten += Math.pow(Math.E, -1 * Math.PI * freq * tstar);
                }
                atten = atten / N;
            }
        } else {
            atten = 1;
        }
        return atten;
    }


    /**
     * Calculate t* over path at the given frequency. See eq B13.2.2 in FMGS, p374.
     */
    public double calcTStar() {
        return getPhase().calcTstar(this);
    }

    public double calcPathLength() {
        // path length
        double length = 0;
        TauModel tMod = getPhase().getTauModel();
        double R = tMod.radiusOfEarth;
        List<ArrivalPathSegment> pathSegList = getPathSegments();
        TimeDist prev = new TimeDist();
        for (ArrivalPathSegment pseg : pathSegList) {
            if (pseg.getPhaseSegment().getIsFlat()) {
                // flat so calc circle length at depth

                for (TimeDist td : pseg.getPath()) {
                    double radianInc = td.getDistRadian() - prev.getDistRadian();
                    length += radianInc*(R-td.getDepth());
                }
            } else {
                // normal segment
                for (TimeDist td : pseg.getPath()) {
                    double radianInc = td.getDistRadian() - prev.getDistRadian();
                    double radius_a = R - prev.getDepth();
                    double radius_b = R - td.getDepth();
                    double pathInc = Math.sqrt(radius_a * radius_a + radius_b * radius_b - 2 * radius_a * radius_b * Math.cos(radianInc));
                    length += pathInc;
                    prev = td;
                }
            }
        }

        return length;
    }

    /**
     * returns path points as TimeDist objects.
     * */
    public TimeDist[] getPath() {
        if (pathSegments == null) {
            this.pathSegments = getPhase().calcSegmentPaths(this);

        }
        List<TimeDist> pathList = new ArrayList<>();
        for (ArrivalPathSegment seg : pathSegments) {
            pathList.addAll(seg.path);
        }
        return pathList.toArray(new TimeDist[0]);
    }

    public List<ArrivalPathSegment> getPathSegments() {
        if (pathSegments == null) {
            pathSegments = getPhase().calcSegmentPaths(this);
        }
        return this.pathSegments;
    }

    /**
     * Negates the arrival distance. Primarily used when printing a scatter arrival that is at negative distance.
     * No other fields are changed.
     * @return new Arrival with dist and search dist negated
     */
    public Arrival negateDistance() {
        return new Arrival( phase,
            time,
            -1* dist,
            rayParam,
            rayParamIndex,
            searchCalc,
            name,
            puristName,
            sourceDepth,
            receiverDepth,
                takeoffAngleRadian,
                incidentAngleRadian,
            dRPdDist);
    }

    public boolean isRelativeToArrival() {
        return relativeToArrival != null;
    }

    public Arrival getRelativeToArrival() {
        return relativeToArrival;
    }

    public void setRelativeToArrival(Arrival relativeToArrival) {
        this.relativeToArrival = relativeToArrival;
    }

    public static String toStringHeader() {
        return "Dist(deg)  Source  Name  Time    RayParam  Takeoff  Incident  PureDist  = PureName";
    }
    public String toString() {
        double moduloDistDeg = getModuloDistDeg();
        if (getSearchDistDeg() < 0) {
            // search in neg distance, likely for scatter
            moduloDistDeg *= -1;
        }
        String desc =  Outputs.formatDistance(moduloDistDeg) + Outputs.formatDepth(getSourceDepth()) + "   " + getName()
                + "  " + Outputs.formatTime(getTime()) + "  " + Outputs.formatRayParam(Math.PI / 180.0 * getRayParam())
                + "  " + Outputs.formatDistance(getTakeoffAngleDegree()) + " " + Outputs.formatDistance(getIncidentAngleDegree())
                + " " + Outputs.formatDistance(getDistDeg())+" "+getRayParamIndex();
        if (getName().equals(getPuristName())) {
            desc += "   = ";
        } else {
            desc += "   * ";
        }
        desc += getPuristName();
        if (getRayCalculateable().hasDescription()) {
            desc += " "+getRayCalculateable().getDescription();
        }
        return desc;
    }

    /**
     * Create TimeDist point for source, first point in pierce or path.
     */
    public TimeDist getSourceTimeDist() {
        return new TimeDist(getRayParam(), 0, 0, getSourceDepth());
    }
    public int getNumPiercePoints() {
        return getPierce().length;
    }

    public int getNumPathPoints() {
        int c = 0;
        for (ArrivalPathSegment seg : getPathSegments()) {
            c += seg.path.size();
        }
        return c;
    }

    public TimeDist getPiercePoint(int i) {
        // don't check for i> length since we want an ArrayOutOfBounds anyway
        return getPierce()[i];
    }

    /**
     * finds the first pierce point at the given depth.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if depth is not found
     */
    public TimeDist getFirstPiercePoint(double depth) {
        for (TimeDist timeDist : getPierce()) {
            if (timeDist.getDepth() == depth) {
                return timeDist;
            }
        }
        throw new ArrayIndexOutOfBoundsException("No Pierce point found for depth "
                + depth);
    }

    /**
     * finds the last pierce point at the given depth.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if depth is not found
     */
    public TimeDist getLastPiercePoint(double depth) {
        TimeDist piercepoint = null;
        for (TimeDist timeDist : getPierce()) {
            if (timeDist.getDepth() == depth) {
                piercepoint = timeDist;
            }
        }
        if(piercepoint == null) {
            throw new ArrayIndexOutOfBoundsException("No Pierce point found for depth "
                    + depth);
        }
        return piercepoint;
    }


    public static final float DEFAULT_ATTENUATION_FREQUENCY = 1.0f;

    protected static final double KMtoM = 1000.0; // 1000 m per km

    protected static final double MgtoKg = 1.0/1000;

    public static Arrival getEarliestArrival(List<Arrival> arrivals) {
        double soonest = Double.MAX_VALUE;
        Arrival soonestArrival = null;
        for (Arrival a : arrivals) {
            if (a.getTime() < soonest) {
                soonestArrival = a;
                soonest = a.getTime();
            }
        }
        return soonestArrival;
    }
    public static Arrival getLatestArrival(List<Arrival> arrivals) {
        double latest = -1*Double.MAX_VALUE;
        Arrival latestArrival = null;
        for (Arrival a : arrivals) {
            if (a.getTime() > latest) {
                latestArrival = a;
                latest = a.getTime();
            }
        }
        return latestArrival;
    }

    public void writeJSON(PrintWriter pw, String indent, boolean withAmp) {
        writeJSON(pw, indent, false, false, withAmp, MomentMagnitude.MAG4_MOMENT, DEFAULT_ATTENUATION_FREQUENCY);
    }

    public void writeJSON(PrintWriter pw, String indent,
                          boolean withPierce,
                          boolean withPath,
                          boolean withAmp, double moment, double attenuationFrequency) {
        String NL = "\n";
        pw.write(indent+"{"+NL);
        String innerIndent = indent+"  ";

        pw.write(innerIndent+JSONWriter.valueToString("sourcedepth")+": "+JSONWriter.valueToString((float)getSourceDepth())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("distdeg")+": "+JSONWriter.valueToString((float)getModuloDistDeg())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("phase")+": "+JSONWriter.valueToString(getName())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("time")+": "+JSONWriter.valueToString((float)getTime())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("rayparam")+": "+JSONWriter.valueToString((float)(Math.PI / 180.0 * getRayParam()))+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("takeoff")+": "+JSONWriter.valueToString((float) getTakeoffAngleDegree())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("incident")+": "+JSONWriter.valueToString((float) getIncidentAngleDegree())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("puristdist")+": "+JSONWriter.valueToString((float)getDistDeg())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("puristname")+": "+JSONWriter.valueToString(getPuristName()));
        if (getRayCalculateable().hasDescription()) {
            pw.write(","+NL);
            pw.write(innerIndent+JSONWriter.valueToString("desc")+": "+JSONWriter.valueToString(getRayCalculateable().getDescription()));
        }

        if (withAmp) {

            pw.write(","+NL);
            pw.write(innerIndent + JSONWriter.valueToString("amp") + ": {" + NL);

            try {
                VelocityModel vMod = getPhase().getTauModel().getVelocityModel();
                VelocityLayer top = vMod.getVelocityLayer(0);
                double freeFactor = 1.0;
                if (getReceiverDepth() <= 1.0) {
                    Complex[] freeSurfRF;
                    ReflTransFreeSurface rtFree = ReflTransFreeSurface.createReflTransFreeSurface(top.getTopPVelocity(), top.getTopSVelocity(), top.getTopDensity());
                    if (getPhase().getFinalPhaseSegment().isPWave) {
                        freeSurfRF = rtFree.getFreeSurfaceReceiverFunP(getRayParam() / vMod.getRadiusOfEarth());
                    } else {
                        freeSurfRF = rtFree.getFreeSurfaceReceiverFunSv(getRayParam() / vMod.getRadiusOfEarth());
                    }
                    freeFactor = Complex.abs(Complex.sqrt(freeSurfRF[0].times(freeSurfRF[0].plus(freeSurfRF[1].times(freeSurfRF[1])))));
                }
                double geoSpread = getAmplitudeGeometricSpreadingFactor(); // 1/km
                // km/s
                double sourceVel = getPhase().velocityAtSource(); // km/s
                // Mg/m3 * (km/s)3 => 1e3 Kg/m3 * 1e9 (m3/s3) => 1e12 Kg /s3
                double radiationTerm = 4*Math.PI*getPhase().densityAtSource()*sourceVel*sourceVel*sourceVel*1e12;
                double attenuation = calcAttenuation();
                //                          Kg m2 / s2     1        1/km   /   ( Kg/s3 )
                // attenuation * freeFactor* moment * refltran * geoSpread / radiationTerm / 1e3; // s m2/km => s m / 1e3  why sec???
                SeismicSourceArgs sourceArgs = getRayCalculateable().getSourceArgs();
                double[] radiationPattern = sourceArgs.calcRadiationPat( searchCalc.azimuth, getTakeoffAngleDegree());

                if (Double.isFinite(geoSpread)) {
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("factorpsv") + ": " + JSONWriter.valueToString((float) getAmplitudeFactorPSV()) + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("factorsh") + ": " + JSONWriter.valueToString((float) getAmplitudeFactorSH()) + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("geospread") + ": " + JSONWriter.valueToString((float) geoSpread) + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("attenuation") + ": " + JSONWriter.valueToString((float) attenuation) + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("freeFactor") + ": " + JSONWriter.valueToString((float) freeFactor) + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("moment") + ": " + JSONWriter.valueToString((float) moment) + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("radiationPattern") + ": ["
                            + (float) radiationPattern[0]+", "
                            + (float) radiationPattern[1]+", "
                            + (float) radiationPattern[2]+"] "
                            + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("radiationTerm") + ": " + JSONWriter.valueToString((float) radiationTerm) + "," + NL);
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("unitconv") + ": " + JSONWriter.valueToString((float) 1e-3) + "," + NL);
                } else {
                    pw.write(innerIndent + "  " + JSONWriter.valueToString("error") + ": " + JSONWriter.valueToString("geometrical speading not finite") + "," + NL);
                }

                pw.write(innerIndent + "  " + JSONWriter.valueToString("refltranpsv") + ": " + JSONWriter.valueToString((float) getEnergyFluxFactorReflTransPSV()) + ", " + NL);
                pw.write(innerIndent + "  " + JSONWriter.valueToString("refltransh") + ": " + JSONWriter.valueToString((float) getEnergyFluxFactorReflTransSH()) + NL);
                pw.write(innerIndent + "}");
            } catch (TauPException e) {
                throw new RuntimeException(e);
            }
        }

        if (getPhase() instanceof ScatteredSeismicPhase) {
            pw.write(","+NL);
            ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase)getPhase();
            pw.write(innerIndent+JSONWriter.valueToString("scatter")+": {"+NL);
            pw.write(innerIndent+"  "+JSONWriter.valueToString("depth")+": "+JSONWriter.valueToString((float)scatPhase.getScattererDepth())+","+NL);
            pw.write(innerIndent+"  "+JSONWriter.valueToString("distdeg")+": "+JSONWriter.valueToString((float)scatPhase.getScattererDistanceDeg())+","+NL);
            pw.write(innerIndent+"}");
        }
        if (isRelativeToArrival()) {
            pw.write(","+NL);
            Arrival relArrival = getRelativeToArrival();
            pw.write(innerIndent+JSONWriter.valueToString("relative")+": {"+NL);
            pw.write(innerIndent+"  "+JSONWriter.valueToString("difference")+": "+JSONWriter.valueToString((float)(getTime()-relArrival.getTime()))+","+NL);
            pw.write(innerIndent+"  "+JSONWriter.valueToString("arrival")+": "+NL);
            relArrival.writeJSON(pw, innerIndent+"  "+"  ", withPierce, withPath, withAmp, moment, attenuationFrequency);
            pw.write(innerIndent+"}");
        }
        if (withPierce ) {
            pw.write(","+NL);
            pw.write(innerIndent+JSONWriter.valueToString("pierce")+": ["+NL);
            TimeDist[] tdArray = getPierce();
            boolean first = true;
            for (TimeDist td : tdArray) {
                if (!first) {
                    pw.write(","+NL);
                } else {
                    first = false;
                }
                pw.write(innerIndent+"  [ "+
                        JSONWriter.valueToString((float)td.getDistDeg())+", "+
                        JSONWriter.valueToString((float)td.getDepth())+", "+
                        JSONWriter.valueToString((float)td.getTime())+" ]");
            }
            pw.write(NL);
            pw.write(innerIndent+"]");
        }
        if (withPath ) {
            pw.write(","+NL);
            pw.write(innerIndent+JSONWriter.valueToString("pathlength")+": "+((float)calcPathLength())+","+NL);
            pw.write(innerIndent+JSONWriter.valueToString("path")+": ["+NL);
            boolean first = true;
            for (ArrivalPathSegment seg : pathSegments) {
                if (!first) {
                    pw.write(","+NL);
                } else {
                    first = false;
                }
                seg.writeJSON(pw, innerIndent);
            }
            pw.write(NL+innerIndent+"]");
        }
        pw.write(NL);
        pw.write(indent+"}"); // main end
    }

    public static String CSVHeader() {
        return "Model,Distance (deg),Depth (km),Phase,Time (s),RayParam (deg/s),Takeoff Angle,Incident Angle,Purist Distance,Purist Name,Recv Depth";
    }

    public String asCSVRow() {
        String sep = ",";
        String modelName = getPhase().getTauModel().getModelName().replaceAll("\"", " ");
        if (modelName.contains(",")) {
            modelName = "\""+modelName+"\"";
        }
        String line = modelName + sep
                + Outputs.formatDistance(getModuloDistDeg()).trim() + sep
                + Outputs.formatDepth(getSourceDepth()).trim() + sep
                + getName().trim() + sep
                + Outputs.formatTime(getTime()).trim() + sep
                + Outputs.formatRayParam(getRayParamDeg()).trim() + sep
                + Outputs.formatDistance(getTakeoffAngleDegree()).trim() + sep
                + Outputs.formatDistance(getIncidentAngleDegree()).trim() + sep
                + Outputs.formatDistance(getDistDeg()).trim() + sep
                + getPuristName().trim()
                + receiverDepth;
        return line;
    }


    public JSONObject asJSONObject() {
        JSONObject a = new JSONObject();
        a.put("distdeg", (float)getModuloDistDeg());
        a.put("sourcedepth", (float)getSourceDepth());
        a.put("phase", getName());
        a.put("time", (float)getTime());
        a.put("rayparam", (float)(Math.PI / 180.0 * getRayParam()));
        a.put("takeoff", (float) getTakeoffAngleDegree());
        a.put("incident", (float) getIncidentAngleDegree());
        a.put("puristdist", (float)getDistDeg());
        a.put("puristname", getPuristName());
        if (getRayCalculateable().hasDescription()) {
            a.put("desc", getRayCalculateable().getDescription());
        }
        JSONObject ampObj = new JSONObject();
        a.put("amp", ampObj);
        try {
            double geospread = getAmplitudeGeometricSpreadingFactor();
            if (Double.isFinite(geospread)) {
                SeismicSourceArgs sourceArgs = getRayCalculateable().sourceArgs;
                ampObj.put("factorpsv", (float) getAmplitudeFactorPSV());
                ampObj.put("factorsh", (float) getAmplitudeFactorSH());
                ampObj.put("geospread", (float) geospread);
                ampObj.put("source", getRayCalculateable().getSourceArgs().asJSONObject());
            } else {
                ampObj.put("error", "geometrical speading not finite");
            }
            ampObj.put("refltranpsv", (float) getEnergyFluxFactorReflTransPSV());
            ampObj.put("refltransh", (float) getEnergyFluxFactorReflTransSH());
        } catch (TauPException e) {
            throw new RuntimeException(e);
        }
        if (getPhase() instanceof ScatteredSeismicPhase) {
            ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase)getPhase();
            a.put("scatterdepth", (float)scatPhase.getScattererDepth());
            a.put("scatterdistdeg", scatPhase.getScattererDistanceDeg());
        }
        if (isRelativeToArrival()) {
            Arrival relArrival = getRelativeToArrival();
            JSONObject relA = new JSONObject();
            a.put("relative", relA);
            relA.put("difference", (float)(getTime()-relArrival.getTime()));
            relA.put("arrival", relArrival.asJSONObject());
        }
        if (pierce != null) {
            JSONArray points = new JSONArray();
            a.put("pierce", points);
            TimeDist[] tdArray = getPierce();
            for (TimeDist td : tdArray) {
                JSONArray tdItems = new JSONArray();
                points.put(tdItems);
                tdItems.put(td.getDistDeg());
                tdItems.put(td.getDepth());
                tdItems.put(td.getTime());
                if (isLatLonable()) {
                    double[] latlon = getLatLonable().calcLatLon(td.getDistDeg(), getDistDeg());
                    tdItems.put(latlon[0]);
                    tdItems.put(latlon[1]);
                }
            }
        }
        if (pathSegments != null) {
            a.put("pathlength", calcPathLength());
            JSONArray points = new JSONArray();
            a.put("path", points);
            for (ArrivalPathSegment seg : pathSegments) {
                points.put(seg.asJSONObject());
            }
        }
        return a;
    }

    public RayCalculateable getRayCalculateable() {
        return this.searchCalc;
    }

    public boolean isLatLonable() {
        return this.getRayCalculateable() != null && this.getRayCalculateable().isLatLonable() ;
    }
    public LatLonable getLatLonable() {
        return getRayCalculateable() != null ? getRayCalculateable().getLatLonable() : null;
    }

}
