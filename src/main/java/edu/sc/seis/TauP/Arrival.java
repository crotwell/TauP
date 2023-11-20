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

import java.time.Duration;
import java.util.List;

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
                dist,
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
                   double searchDist,
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
        this.searchDist = searchDist;
        this.searchDistDeg = searchDist * RtoD; // likely rounding errors, but close and should be reset by calc fn
        this.name = name;
        this.puristName = puristName;
        this.sourceDepth = sourceDepth;
        this.receiverDepth = receiverDepth;
        this.takeoffAngle = takeoffAngle;
        this.incidentAngle = incidentAngle;
        this.dRPdDist = dRPdDist;
    }


    /** phase that generated this arrival. */
    private SeismicPhase phase;

    /** travel time in seconds */
    private double time;

    /** angular distance (great circle) in radians */
    private double dist;

    /** ray parameter in seconds per radians. */
    private double rayParam;

    private int rayParamIndex;

    private double dRPdDist;

    /** original angular search distance (great circle) in radians. May differ from dist by multiple of 2 pi
     * or be pi - dist for long way around. */
    private double searchDist;
    /** original angular search distance (great circle) in degrees. May differ from dist by multiple of 180
     * or be 360 - dist for long way around. */
    private double searchDistDeg;

    /** phase name */
    private String name;

    /** phase name changed for true depths */
    private String puristName;

    /** source depth in kilometers */
    private double sourceDepth;

    /** receiver depth in kilometers */
    private double receiverDepth;

    /** pierce and path points */
    private TimeDist[] pierce, path;

    private double incidentAngle;
    
    private double takeoffAngle;

    private Arrival relativeToArrival = null;
    
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

    public void setSearchDistDeg(double searchDistDeg) {
        this.searchDistDeg = searchDistDeg;
    }

    /**
     * returns search distance in degrees.
     */
    public double getSearchDistDeg() {
        return this.searchDistDeg;
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
     * See Fundamentals of Modern Global Seismology, ch 13, eq 13.21
     * @return
     * @throws TauModelException
     */
    public double getGeometricSpreadingFactor() throws TauModelException {
        double rofE = getPhase().getTauModel().getRadiusOfEarth();
        double sourceRadius = rofE-getSourceDepth();
        double recRadius = rofE-getReceiverDepth();
        double rpFactor = rayParam;
        double sinFactor = Math.sin(getModuloDist());
        if (getModuloDist() == 0.0 || getModuloDist() == 180.0) {
            // zero dist and antipode have divide by zero,
            return Double.POSITIVE_INFINITY;
        }
        double numerator = velocityAtSource()*rpFactor*Math.abs(getDRayParamDDelta());
        double denominator = velocityAtReceiver()*sourceRadius*sourceRadius
                *recRadius*recRadius
                *radialSlownessAtSource()*radialSlownessAtReceiver()
                *sinFactor;
        return Math.sqrt(numerator/ denominator);
    }

    /**
     * Calculates the product of the reflection and transmission coefficients for all discontinuities along the path
     * of this arrival. Note that this may not give accurate results for certain wave types,
     * such as head or diffracted waves.
     * @return
     * @throws VelocityModelException
     * @throws SlownessModelException
     */
    public double getReflTrans() throws VelocityModelException, SlownessModelException {
        return getPhase().calcReflTran(this);
    }

    /**
     * Calculates the amplitude factor, reflection/tranmission coefficient times geometric spreading, for the
     * arrival. Note this is only an approximation of amplitude as the source radiation magnitude and pattern is
     * not included, and this may not give accurate results for certain wave types, such as head or diffracted waves.
     * @return
     * @throws TauModelException
     * @throws VelocityModelException
     * @throws SlownessModelException
     */
    public double getAmplitudeFactor() throws TauModelException, VelocityModelException, SlownessModelException {
        double refltran = getReflTrans();
        double geoSpread = getGeometricSpreadingFactor();
        double ampFactor = refltran * geoSpread;
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
        return Math.sqrt(1/(srcVel*srcVel) - getRayParam()*getRayParam()/(srcRadius*srcRadius));
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

    /** returns pierce points as TimeDist objects. */
    public TimeDist[] getPierce() {
        if (pierce == null) {
            this.pierce = getPhase().calcPierceTimeDist(this).toArray(new TimeDist[0]);
        }
        return pierce;
    }

    /** returns path points as TimeDist objects. */
    public TimeDist[] getPath() {
        if (path == null) {
            this.path = getPhase().calcPathTimeDist(this).toArray(new TimeDist[0]);
        }
        return path;
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
         searchDist,
         name,
         puristName,
         sourceDepth,
         receiverDepth,
         takeoffAngle,
         incidentAngle,
                dRPdDist);
        neg.setSearchDistDeg(getSearchDistDeg());
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

    public int getNumPiercePoints() {
        if(pierce != null) {
            return pierce.length;
        } else {
            return 0;
        }
    }

    public int getNumPathPoints() {
        if(path != null) {
            return path.length;
        } else {
            return 0;
        }
    }

    public TimeDist getPiercePoint(int i) {
        // don't check for i> length since we want an ArrayOutOfBounds anyway
        return pierce[i];
    }

    /**
     * finds the first pierce point at the given depth.
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if depth is not found
     */
    public TimeDist getFirstPiercePoint(double depth) {
        for(int i = 0; i < pierce.length; i++) {
            if(pierce[i].getDepth() == depth) {
                return pierce[i];
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
        for(int i = 0; i < pierce.length; i++) {
            if(pierce[i].getDepth() == depth) {
                piercepoint = pierce[i];
            }
        }
        if(piercepoint == null) {
            throw new ArrayIndexOutOfBoundsException("No Pierce point found for depth "
                    + depth);
        }
        return piercepoint;
    }

    public TimeDist getPathPoint(int i) {
        // don't check for i> length since we want an ArrayOutOfBounds anyway
        return path[i];
    }

    protected static final double TWOPI = 2.0 * Math.PI;

    protected static final double DtoR = Math.PI / 180.0;

    protected static final double RtoD = 180.0 / Math.PI;

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
        double latest = Double.MAX_VALUE;
        Arrival latestArrival = null;
        for (Arrival a : arrivals) {
            if (a.getTime() < latest) {
                latestArrival = a;
                latest = a.getTime();
            }
        }
        return latestArrival;
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
                ampObj.put("factor", (float) getAmplitudeFactor());
                ampObj.put("geospread", (float) geospread);
            } else {
                ampObj.put("error", "geometrical speading not finite");
            }
            ampObj.put("refltran", (float) getReflTrans());
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
        if (pierce != null || path != null) {
            JSONArray points = new JSONArray();
            a.put("pierce", points);
            boolean first = true;
            TimeDist[] tdArray = getPierce();
            for (TimeDist td : tdArray) {
                JSONArray tdItems = new JSONArray();
                points.put(tdItems);
                tdItems.put(td.getDistDeg());
                tdItems.put(td.getDepth());
                tdItems.put(td.getTime());
            }
        }
        if (path != null) {
            JSONArray points = new JSONArray();
            a.put("path", points);
            TimeDist[] tdArray = getPath();
            for (TimeDist td : tdArray) {
                JSONArray tdItems = new JSONArray();
                points.put(tdItems);
                tdItems.put(td.getDistDeg());
                tdItems.put(td.getDepth());
                tdItems.put(td.getTime());
            }
        }
        return a;
    }

    public String asJSON(boolean pretty, String indent) {
        return asJSON(pretty, indent, false, false);
    }

    public String asJSON(boolean pretty, String indent, boolean withPierce, boolean withPath) {
        String NL = "";
        if (pretty) {
            NL = "\n";
        }
        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SS = S+S;
        String SQ = S+Q;
        String SSQ = S+SQ;
        StringBuilder out = new StringBuilder();
        out.append(indent+"{"+NL);
        out.append(indent+SQ+"distdeg"+QC+(float)getModuloDistDeg()+COMMA+NL);
        out.append(indent+SQ+"phase"+QCQ+getName()+QCOMMA+NL);
        out.append(indent+SQ+"time"+QC+(float)getTime()+COMMA+NL);
        out.append(indent+SQ+"rayparam"+QC+(float)(Math.PI / 180.0 * getRayParam())+COMMA+NL);
        out.append(indent+SQ+"takeoff"+QC+(float)getTakeoffAngle()+COMMA+NL);
        out.append(indent+SQ+"incident"+QC+(float)getIncidentAngle()+COMMA+NL);
        out.append(indent+SQ+"puristdist"+QC+(float)getDistDeg()+COMMA+NL);
        out.append(indent+SQ+"puristname"+QCQ+getPuristName()+Q);
        if (getPhase() instanceof ScatteredSeismicPhase) {
            ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase)getPhase();
            out.append(COMMA+NL);
            out.append(indent+SQ+"scatterdepth"+QC+(float)scatPhase.getScattererDepth()+COMMA+NL);
            out.append(indent+SQ+"scatterdistdeg"+QC+scatPhase.getScattererDistanceDeg());
        }
        if (isRelativeToArrival()) {
            out.append(COMMA+NL);
            Arrival relArrival = getRelativeToArrival();
            out.append(indent+SQ+"relative"+QC+"{"+NL);
            out.append(indent+S+SQ+"difference"+QC+(float)(getTime()-relArrival.getTime())+COMMA+NL);
            out.append(indent+S+SQ+"arrival"+QC+relArrival.asJSON(pretty,indent+S+S)+COMMA+NL);
            out.append(indent+S+"}");
        }
        if (withPierce || withPath) {
            out.append(COMMA+NL);
            String key = withPierce ? "pierce" : "path";
            out.append(indent+SQ+key+QC+"[");
            boolean first = true;
            TimeDist[] tdArray = withPierce ? getPierce() : getPath();
            for (TimeDist td : tdArray) {
                if (first) {
                    first = false;
                    out.append(NL);
                } else {
                    out.append(COMMA+NL);
                }
                out.append(indent+"  ["+Q+(float)td.getDistDeg()+QCOMMA+Q+(float)td.getDepth()+QCOMMA+Q+(float)td.getTime()+Q+"]");
            }
            out.append(NL);
            out.append(indent+"]");
        } else {
            out.append(NL);
        }
        out.append(indent+"}");
        return out.toString();
    }

}
