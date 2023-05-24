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
                   int rayParamIndex) {
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
                phase.calcIncidentAngle(rayParam));
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
                   double incidentAngle) {

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
        this.name = name;
        this.puristName = puristName;
        this.sourceDepth = sourceDepth;
        this.receiverDepth = receiverDepth;
        this.takeoffAngle = takeoffAngle;
        this.incidentAngle = incidentAngle;
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

    public double getIncidentAngle() {
        return incidentAngle;
    }
    
    public double getTakeoffAngle() {
        return takeoffAngle;
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

    public String toString() {
        String desc =  Outputs.formatDistance(getModuloDistDeg()) + Outputs.formatDepth(getSourceDepth()) + "   " + getName()
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

    public String asJSON(boolean pretty, String indent) {
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
            out.append(indent+SQ+"scatterdistdeg"+QC+scatPhase.getScattererDistanceDeg()+NL);
        } else {
            out.append(NL);
        }
        out.append(indent+"}");
        return out.toString();
    }

}
