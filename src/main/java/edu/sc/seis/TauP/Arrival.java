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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.*;

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
                   double takeoffAngle,
                   double incidentAngle,
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
        this.takeoffAngle = takeoffAngle;
        this.incidentAngle = incidentAngle;
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

    private final double incidentAngle;
    
    private final double takeoffAngle;

    private Arrival relativeToArrival = null;

    public static List<Arrival> sortArrivals(List<Arrival> arrivals) {
        Collections.sort(arrivals, new Comparator<Arrival>() {
            public int compare(Arrival o1, Arrival o2) {
                return Double.compare(o1.getTime(), o2.getTime());
            }
        });
        return arrivals;
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
                + " s/deg.";
        if (getPhase().getReceiverDepth() != 0.0) {
            out += " Receiver at depth: "+getPhase().getReceiverDepth()+" km.";
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
        return RtoD * getDist();
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
        return getRayParam()/RtoD;
    }

    public double getDRayParamDDelta() {
        return dRPdDist;
    }

    public double getDRayParamDDeltaDeg() {
        return dRPdDist/RtoD/RtoD;
    }

    /**
     * Geometrical spreading factor.
     * See Fundamentals of Modern Global Seismology, ch 13, eq 13.9.
     * Note that eq 13.10 has divide by zero in case of a horizontal ray leaving the source.
     * @return
     * @throws TauModelException
     */
    public double getGeometricSpreadingFactor() throws TauModelException {
        double rofE = getPhase().getTauModel().getRadiusOfEarth();
        double recRadius = rofE-getReceiverDepth();
        if (getModuloDist() == 0.0 || getModuloDist() == 180.0) {
            // zero dist and antipode have divide by zero,
            return Double.POSITIVE_INFINITY;
        }

        // find neighbor ray
        Arrival neighbor;
        if (getRayParamIndex() == 0) {
            neighbor = getPhase().createArrivalAtIndex(getRayParamIndex()+1);
        } else {
            neighbor = getPhase().createArrivalAtIndex(getRayParamIndex()-1);
        }
        if (neighbor.getDist() == getDist()) {
            // could create better neighbor implementation as shoot ray may give rp that is not same as index???
            throw new TauModelException("Neighbor ray has same dist: "+getPhase().getName()+" "+getDistDeg());
        }
        double dtakeoff_ddelta = (getTakeoffAngle()-neighbor.getTakeoffAngle())*DtoR/
                (getDist()-neighbor.getDist());
        double cosIncident = Math.cos(getIncidentAngle()*DtoR);
        if (Double.isNaN(cosIncident)) {
            // divide by zero???
            return Double.POSITIVE_INFINITY;
        }
        double geoSpread = Math.sin(getTakeoffAngle())* Math.abs(dtakeoff_ddelta)
                / (recRadius * recRadius * cosIncident * Math.sin(getModuloDist()));

        return geoSpread;
    }

    /**
     * Calculates the product of the reflection and transmission coefficients for all discontinuities along the path
     * of this arrival in the P-SV plane. Note that this may not give accurate results for certain wave types,
     * such as head or diffracted waves.
     * @return
     * @throws VelocityModelException
     * @throws SlownessModelException
     */
    public double getReflTransPSV() throws VelocityModelException, SlownessModelException {
        return getPhase().calcReflTranPSV(this);
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
    public double getReflTransSH() throws VelocityModelException, SlownessModelException {
        return getPhase().calcReflTranSH(this);
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
    public double getAmplitudeFactorPSV() throws TauModelException, VelocityModelException, SlownessModelException {
        double refltran = getReflTransPSV();
        double geoSpread = getGeometricSpreadingFactor();
        double densityVelocity = 1/Math.sqrt(getPhase().velocityAtReceiver() * getPhase().densityAtReceiver());
        double ampFactor = densityVelocity* refltran * geoSpread;
        return ampFactor;
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
        double refltran = getReflTransSH();
        double geoSpread = getGeometricSpreadingFactor();
        double densityVelocity = 1/Math.sqrt(getPhase().velocityAtReceiver() * getPhase().densityAtReceiver());
        double ampFactor = densityVelocity* refltran * geoSpread;
        return ampFactor;
    }

    public double getIncidentAngle() {
        return incidentAngle;
    }
    
    public double getTakeoffAngle() {
        return takeoffAngle;
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

    /** returns path points as TimeDist objects.
     *
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
        Arrival neg = new Arrival( phase,
         time,
        -1* dist,
         rayParam,
         rayParamIndex,
         searchCalc,
         name,
         puristName,
         sourceDepth,
         receiverDepth,
         takeoffAngle,
         incidentAngle,
                dRPdDist);
        return neg;
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

    public String toString() {
        double moduloDistDeg = getModuloDistDeg();
        if (getSearchDistDeg() < 0) {
            // search in neg distance, likely for scatter
            moduloDistDeg *= -1;
        }
        String desc =  Outputs.formatDistance(moduloDistDeg) + Outputs.formatDepth(getSourceDepth()) + "   " + getName()
                + "  " + Outputs.formatTime(getTime()) + "  " + Outputs.formatRayParam(Math.PI / 180.0 * getRayParam())
                + "  " + Outputs.formatDistance(getTakeoffAngle()) + " " + Outputs.formatDistance(getIncidentAngle())
                + " " + Outputs.formatDistance(getDistDeg())+" "+getRayParamIndex();
        if (getName().equals(getPuristName())) {
            desc += "   = ";
        } else {
            desc += "   * ";
        }
        desc += getPuristName();
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

    protected static final double DtoR = SphericalCoords.dtor;

    protected static final double RtoD = SphericalCoords.rtod;

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


    public void writeJSON(PrintWriter pw, String indent) throws IOException {
        String NL = "\n";
        pw.write(indent+"{"+NL);
        String innerIndent = indent+"  ";
        pw.write(innerIndent+JSONWriter.valueToString("distdeg")+": "+JSONWriter.valueToString((float)getModuloDistDeg())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("phase")+": "+JSONWriter.valueToString(getName())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("time")+": "+JSONWriter.valueToString((float)getTime())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("rayparam")+": "+JSONWriter.valueToString((float)(Math.PI / 180.0 * getRayParam()))+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("takeoff")+": "+JSONWriter.valueToString((float)getTakeoffAngle())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("incident")+": "+JSONWriter.valueToString((float)getIncidentAngle())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("puristdist")+": "+JSONWriter.valueToString((float)getDistDeg())+","+NL);
        pw.write(innerIndent+JSONWriter.valueToString("puristname")+": "+JSONWriter.valueToString(getPuristName())+","+NL);

        pw.write(innerIndent+JSONWriter.valueToString("amp")+": {"+NL);

        try {
            double geospread = getGeometricSpreadingFactor();
            if (Double.isFinite(geospread)) {
                pw.write(innerIndent+"  "+JSONWriter.valueToString("factorpsv")+": "+JSONWriter.valueToString((float)getAmplitudeFactorPSV())+","+NL);
                pw.write(innerIndent+"  "+JSONWriter.valueToString("factorsh")+": "+JSONWriter.valueToString((float)getAmplitudeFactorSH())+","+NL);
                pw.write(innerIndent+"  "+JSONWriter.valueToString("geospread")+": "+JSONWriter.valueToString((float)geospread)+","+NL);
            } else {
                pw.write(innerIndent+"  "+JSONWriter.valueToString("error")+": "+JSONWriter.valueToString("geometrical speading not finite")+","+NL);
            }
            pw.write(innerIndent+"  "+JSONWriter.valueToString("refltranpsv")+": "+JSONWriter.valueToString((float)getReflTransPSV())+", "+NL);
            pw.write(innerIndent+"  "+JSONWriter.valueToString("refltransh")+": "+JSONWriter.valueToString((float)getReflTransSH())+NL);
            pw.write(innerIndent+"}");
        } catch (TauPException e) {
            throw new RuntimeException(e);
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
            relArrival.writeJSON(pw, innerIndent+"  "+"  ");
            pw.write(innerIndent+"}");
        }
        if (pierce != null ) {
            pw.write(","+NL);
            pw.write(innerIndent+JSONWriter.valueToString("pierce")+": ["+NL);
            TimeDist[] tdArray = getPierce();
            for (TimeDist td : tdArray) {
                pw.write(innerIndent+"  [ "+
                        JSONWriter.valueToString((float)td.getDistDeg())+", "+
                        JSONWriter.valueToString((float)td.getDepth())+", "+
                        JSONWriter.valueToString((float)td.getTime())+" ],"+NL);
            }
            pw.write(innerIndent+"]");
        }
        if (pathSegments != null) {
            pw.write(","+NL);
            pw.write(innerIndent+JSONWriter.valueToString("path")+": ["+NL);
            for (ArrivalPathSegment seg : pathSegments) {
                seg.writeJSON(pw, innerIndent);
                pw.write(","+NL);
            }
            pw.write(innerIndent+"]");
        }
        pw.write(NL);
        pw.write(indent+"}"); // main end
    }

    public JSONObject asJSONObject() {
        JSONObject a = new JSONObject();
        a.put("distdeg", (float)getModuloDistDeg());
        a.put("phase", getName());
        a.put("time", (float)getTime());
        a.put("rayparam", (float)(Math.PI / 180.0 * getRayParam()));
        a.put("takeoff", (float)getTakeoffAngle());
        a.put("incident", (float)getIncidentAngle());
        a.put("puristdist", (float)getDistDeg());
        a.put("puristname", getPuristName());
        JSONObject ampObj = new JSONObject();
        a.put("amp", ampObj);
        try {
            double geospread = getGeometricSpreadingFactor();
            if (Double.isFinite(geospread)) {
                ampObj.put("factorpsv", (float) getAmplitudeFactorPSV());
                ampObj.put("factorsh", (float) getAmplitudeFactorSH());
                ampObj.put("geospread", (float) geospread);
            } else {
                ampObj.put("error", "geometrical speading not finite");
            }
            ampObj.put("refltranpsv", (float) getReflTransPSV());
            ampObj.put("refltransh", (float) getReflTransSH());
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
            }
        }
        if (pathSegments != null) {
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
