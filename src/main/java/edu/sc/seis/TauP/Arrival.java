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
                   SimpleContigSeismicPhase simpleContigSeismicPhase,
                   List<TimeDist> pierce,
                   int rayParamIndex,
                   double dRPdDist) {
        this(phase,
                simpleContigSeismicPhase,
                pierce.get(pierce.size() - 1).getTime(),
                pierce.get(pierce.size() - 1).getDistRadian(),
                pierce.get(pierce.size() - 1).getP(),
                rayParamIndex,
                DistanceRay.ofRadians(pierce.get(pierce.size() - 1).getP()),
                phase.getName(),
                phase.getPuristName(),
                phase.getSourceDepth(),
                phase.getReceiverDepth(),
                phase.calcTakeoffAngle(pierce.get(pierce.size() - 1).getP()),
                phase.calcIncidentAngle(pierce.get(pierce.size() - 1).getP()),
                dRPdDist
                );
        this.pierce = pierce.toArray(new TimeDist[0]);
    }

    public Arrival(SeismicPhase phase,
                   SimpleContigSeismicPhase simpleContigSeismicPhase,
                   double time,
                   double dist,
                   double rayParam,
                   int rayParamIndex,
                   double dRPdDist) {
        this(phase,
                simpleContigSeismicPhase,
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
                dRPdDist
        );
    }
    public Arrival(SeismicPhase phase,
                   SimpleContigSeismicPhase simpleContigSeismicPhase,
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
        if ( ! (phase instanceof SimpleContigSeismicPhase || phase instanceof ScatteredSeismicPhase)) {
            throw new IllegalArgumentException("Phase must be SimpleContigSeismicPhase or ScatteredSeismicPhase");
        }
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
        this.simpleContigSeismicPhase = simpleContigSeismicPhase;
        this.pierce = null;
        this.pathSegments = null;
    }


    /** phase that generated this arrival. */
    private final SimpleContigSeismicPhase simpleContigSeismicPhase;

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

    /**
     * Sorts arrivals by time in place.
     * @param arrivals sorted
     */
    public static void sortArrivals(List<Arrival> arrivals) {
        arrivals.sort(Comparator.comparingDouble(Arrival::getTime));
    }

    /**
     * Sorts arrivals by source and receiver depth in place. Used when calc rel phases for efficiency.
     * @param arrivals sorted
     */
    public static void sortArrivalsBySourceReceiverDepth(List<Arrival> arrivals) {
        arrivals.sort(Comparator.comparingDouble(Arrival::getReceiverDepth));
        arrivals.sort(Comparator.comparingDouble(Arrival::getSourceDepth));
    }

    public static List<Arrival> onlyFirst(List<Arrival> arrivalList) {
        List<Arrival> first = new ArrayList<>();
        List<Arrival> copyList = new ArrayList<>(arrivalList);
        Arrival.sortArrivals(copyList);
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

    /**
     * Returns the contiguous part of the phase that generated this arrival.
     * Usually the same as getPhase(), except in the
     * case of CompositeSeismicPhase where several contiguous phases are separated by shadow zones.
     *
     * @return
     */
    public SimpleContigSeismicPhase getSimpleContigSeismicPhase() {
        if (phase instanceof SimpleContigSeismicPhase) {
            return (SimpleContigSeismicPhase) phase;
        }
        return simpleContigSeismicPhase;
    }

    public boolean isFromCompositePhase() {
        return phase instanceof CompositeSeismicPhase;
    }

    public List<SeismicPhaseSegment> listPhaseSegments() {
        if (phase instanceof SimpleContigSeismicPhase) {
            return ((SimpleContigSeismicPhase)phase).getPhaseSegments();
        }
        if (phase instanceof CompositeSeismicPhase) {
            return getSimpleContigSeismicPhase().getPhaseSegments();
        }
        throw new RuntimeException("SHould not happen, can't find phase segments for arrival");
    }

    /**
     *
     * @return TauModel used to generate the phase that this arrival came from
     */
    public TauModel getTauModel() {
        return phase.getTauModel();
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
            return ((DistanceRay)this.searchCalc).getDegrees(getTauModel().getRadiusOfEarth());
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

    public ArrivalAmplitude calcAmplitude() throws SlownessModelException, TauModelException {
        return new ArrivalAmplitude(this);
    }

    /**
     * Geometrical spreading factor for amplitude, sqrt of energy spreading.
     * See Fundamentals of Modern Global Seismology, ch 13, eq 13.19.
     *
     */
    public double getAmplitudeGeometricSpreadingFactor() {
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
        TauModel tMod = getTauModel();
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
        try {
            return getPhase().calcEnergyFluxFactorReflTranPSV(this);
        } catch (NoArrivalException e) {
            throw new RuntimeException("Should never happen "+getName(), e);
        }
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
        try {
            return getPhase().calcEnergyFluxFactorReflTranSH(this);
        } catch (NoArrivalException e) {
            throw new RuntimeException("Should never happen "+getName(), e);
        }
    }

    /**
     * Calculates the amplitude factor, 1/sqrt(density*vel) times reflection/tranmission coefficient times geometric spreading, for the
     * arrival. Note this is only an approximation of amplitude as the source radiation magnitude and pattern is
     * not included, and this may not give accurate results for certain wave types, such as head or diffracted waves.
     * <p>
     * See FMGS eq 17.74
     */
    public double getAmplitudeFactorPSV() throws TauModelException, SlownessModelException {
        SeismicSource sourceArgs = getRayCalculateable().getSeismicSource();

        if (sourceArgs == null) {
            throw new TauModelException("sourceArgs is null, RayCalc:"+getRayCalculateable());
        }

        double ampFactor = getAmplitudeFactorPSV(getRayCalculateable().getMoment(), sourceArgs.getAttenuationFrequency(), sourceArgs.getNumFrequencies());
        if (getRayCalculateable().hasFaultPlane() && getRayCalculateable().hasAzimuth() ) {
            FaultPlane faultPlane = getRayCalculateable().getFaultPlane();
            RadiationAmplitude radiationPattern = faultPlane.calcRadiationPatDegree( searchCalc.getAzimuth(), getTakeoffAngleDegree());
            double radTerm = 1;
            if (getPhase().getInitialPhaseSegment().isPWave) {
                radTerm = radiationPattern.getRadialAmplitude();
            } else {
                radTerm = radiationPattern.getPhiAmplitude();
            }
            ampFactor *= radTerm;
        } else if (getRayCalculateable().hasFaultPlane() && (!searchCalc.hasAzimuth()) ) {
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
            VelocityModel vMod = getTauModel().getVelocityModel();
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
        double radiationTerm = calcRadiationTerm();
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
        SeismicSource sourceArgs = getRayCalculateable().getSeismicSource();
        double ampFactor = getAmplitudeFactorSH(getRayCalculateable().getMoment(), sourceArgs.getAttenuationFrequency(), sourceArgs.getNumFrequencies());
        if (getRayCalculateable().hasFaultPlane() && getRayCalculateable().hasAzimuth() ) {
            FaultPlane faultPlane = getRayCalculateable().getFaultPlane();
            RadiationAmplitude radiationPattern = faultPlane.calcRadiationPatDegree( getRayCalculateable().getAzimuth(), getTakeoffAngleDegree());
            ampFactor *= radiationPattern.getThetaAmplitude();
        } else if (getRayCalculateable().hasFaultPlane() && !getRayCalculateable().hasAzimuth() ) {
            // change to TauPException
            throw new TauModelException("Amplitude with Strike-dip-rake requires azimuth: "+getRayCalculateable());
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
        double radiationTerm = calcRadiationTerm();
        double attenuation=1;
        if (attenuationFrequency > 0) {
            attenuation = calcAttenuation(attenuationFrequency, numFreq);
        }
        double freeFactor =  1.0;
        if (getReceiverDepth() <= 1.0) {
            // should be exactly 2.0, but go through the steps so looks like PSv case
            VelocityModel vMod = getTauModel().getVelocityModel();
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
        double radTerm = 1;
        if (getRayCalculateable().hasFaultPlane() && getRayCalculateable().hasAzimuth() ) {
            FaultPlane faultPlane = getRayCalculateable().getFaultPlane();
            RadiationAmplitude radiationPattern = faultPlane.calcRadiationPatDegree( getRayCalculateable().getAzimuth(), getTakeoffAngleDegree());
            if (getPhase().getInitialPhaseSegment().isPWave) {
                radTerm = radiationPattern.getRadialAmplitude();
            } else {
                radTerm = radiationPattern.getPhiAmplitude();
            }
        }
        return radTerm;
    }

    public double getRadiationPatternSH() {
        double radTerm = 1;
        if (getRayCalculateable().hasFaultPlane() && getRayCalculateable().hasAzimuth() ) {
            FaultPlane faultPlane = getRayCalculateable().getFaultPlane();
            RadiationAmplitude radiationPattern = faultPlane.calcRadiationPatDegree( getRayCalculateable().getAzimuth(), getTakeoffAngleDegree());
            if (getPhase().getInitialPhaseSegment().isPWave) {
                radTerm = 0;
            } else {
                radTerm = radiationPattern.getThetaAmplitude();
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
        double rofE = getTauModel().getRadiusOfEarth();
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
        double rofE = getTauModel().getRadiusOfEarth();
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
            try {
                this.pierce = getPhase().interpPierceTimeDist(this).toArray(new TimeDist[0]);
            } catch (NoArrivalException | TauModelException e) {
                throw new RuntimeException("Should never happen "+getName(), e);
            }
        }
        return pierce;
    }

    public double calcFreeFactor() throws VelocityModelException {
        VelocityModel vMod = getTauModel().getVelocityModel();
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
        return freeFactor;
    }

    /**
     * Calculate attenuation over path at the default frequency. See eq B13.2.2 in FMGS, p374.
     */
    public double calcAttenuation() {
        return calcAttenuation(getRayCalculateable().getAttenuationFrequency(),
                getRayCalculateable().getNumFrequencies());
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
        try {
            return getPhase().calcTstar(this);
        } catch (NoArrivalException e) {
            throw new RuntimeException("Should never happen "+getName(), e);
        }
    }

    /**
     * Calculate radiation pattern terms, Fp, Fsv, Fsh for the given fault orientation and az,takeoff.
     *
     * @return Fp, Fsv, Fsh
     */
    public RadiationAmplitude calcRadiationPattern() {
        RadiationAmplitude radiationPattern = new RadiationAmplitude(
                new SphericalCoordinate(0,0),
                new double[] {1,1,1});
        if (getRayCalculateable().hasFaultPlane() && getRayCalculateable().hasAzimuth()) {
            FaultPlane faultPlane = getRayCalculateable().getFaultPlane();
            radiationPattern = faultPlane.calcRadiationPatDegree( getRayCalculateable().getAzimuth(), getTakeoffAngleDegree());
        }
        return radiationPattern;
    }

    public double calcRadiationTerm() {
        double sourceVel = getPhase().velocityAtSource();
        double radiationTerm = 4*Math.PI*getPhase().densityAtSource()*sourceVel*sourceVel*sourceVel*1e12;
        return radiationTerm;
    }

    /**
     * Calculates the path length.
     * @return path length in kilometers
     */
    public double calcPathLength() {
        // path length
        double length = 0;
        TauModel tMod = getTauModel();
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
            try {
                this.pathSegments = getPhase().calcSegmentPaths(this);
            } catch (NoArrivalException | SlownessModelException | TauModelException e) {
                throw new RuntimeException("Should never happen "+getName(), e);
            }

        }
        List<TimeDist> pathList = new ArrayList<>();
        for (ArrivalPathSegment seg : pathSegments) {
            pathList.addAll(seg.path);
        }
        return pathList.toArray(new TimeDist[0]);
    }

    public List<ArrivalPathSegment> getPathSegments() {
        if (pathSegments == null) {
            try {
                pathSegments = getPhase().calcSegmentPaths(this);
            } catch (NoArrivalException | SlownessModelException | TauModelException e) {
                throw new RuntimeException("Should never happen "+getName(), e);
            }
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
                simpleContigSeismicPhase, time,
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
                dRPdDist
        );
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
                + " " + Outputs.formatDistance(getDistDeg());
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

    public static List<String> headerNames(boolean tableStyle) {
        List<String> common = List.of("Distance (deg)","Depth (km)","Phase","Time (s)","RayParam (deg/s)",
                "Takeoff Angle","Incident Angle","Recv Depth","Purist Distance","Purist Name");
        if (tableStyle) {
            List<String> line = new ArrayList<>(List.of("Model"));
            line.addAll(common);
            return line;
        }
        return common;
    }
    public static String CSVHeader() {
        return String.join(",", headerNames(true));
    }

    /**
     * Creates a List of Strings, each corresponding to the headers via headerNames() for this arrival.
     * @return arrival for printing
     */
    public List<String> asStringList(boolean tableStyle, String phaseFormat, String phasePuristFormat,
                                     boolean withAmp, boolean withRelPhase) {
        List<String> line = new ArrayList<>();
        if (tableStyle) {
            String modelName = getTauModel().getModelName().replaceAll("\"", " ");
            if (modelName.contains(",")) {
                modelName = "\"" + modelName + "\"";
            }
            line.add(modelName);
        }
        line.addAll(List.of(
                Outputs.formatDistance(tableStyle ? getModuloDistDeg() : getSearchDistDeg()),
                Outputs.formatDepth(getSourceDepth()) ,
                "   "+String.format(phaseFormat, getName()),
                "  "+Outputs.formatTime(getTime()),
                "  "+Outputs.formatRayParam(getRayParamDeg()),
                "  "+Outputs.formatDistance(getTakeoffAngleDegree()),
                " "+Outputs.formatDistance(getIncidentAngleDegree()),
                " "+Outputs.formatDepth(getReceiverDepth()),
                " "+Outputs.formatDistance(getDistDeg())));
        if (! tableStyle) {
            String puristSame = (getName().equals(getPuristName())?"   = " : "   * ");
            line.add(puristSame);
        }
        line.add(String.format(phasePuristFormat, getPuristName()));
        if (withAmp) {
            try {
                double ampFactorPSV = getAmplitudeFactorPSV();
                double ampFactorSH = getAmplitudeFactorSH();
                line.addAll(List.of(" " + Outputs.formatAmpFactor(ampFactorPSV),
                        " " + Outputs.formatAmpFactor(ampFactorSH)));
            } catch (SlownessModelException | TauModelException e) {
                throw new RuntimeException("Should not happen", e);
            }
        }
        if (withRelPhase) {
            if (isRelativeToArrival()) {
                line.add(" " + Outputs.formatTime(getTime() - getRelativeToArrival().getTime()));
                line.add(" +" + String.format(phaseFormat, getRelativeToArrival().getName()));
            } else {
                line.add(String.format(phaseFormat, " no arrival"));
                line.add("");
            }
        }
        if (getRayCalculateable().hasDescription()) {
            line.add(" "+getRayCalculateable().getDescription());
        }
        return line;
    }


    public String asCSVRow() {
        String sep = ",";
        StringBuilder sb = new StringBuilder();
        for (String s : asStringList(true, "%s", "%s", false, false)) {
            sb.append(s.trim()).append(sep);
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
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
