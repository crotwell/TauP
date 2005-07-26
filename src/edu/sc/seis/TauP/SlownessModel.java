/*
  The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
  Copyright (C) 1998-2000 University of South Carolina

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

  The current version can be found at 
  <A HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>

  Bug reports and comments should be directed to 
  H. Philip Crotwell, crotwell@seis.sc.edu or
  Tom Owens, owens@seis.sc.edu

*/

package edu.sc.seis.TauP;

import java.io.Serializable;
import java.util.Vector;

/**
  * This class provides storage and methods for generating slowness-depth
  * pairs.
  *
  * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



  * @author H. Philip Crotwell
  *
  */
public abstract class SlownessModel implements Serializable, Cloneable {

    /** True to enable debugging output. */
    transient public boolean DEBUG = false;

    /** True to enable verbose output. */
    transient public boolean verbose = false;

    /** Radius of the Earth in km, usually input from the velocity model. */
    protected double radiusOfEarth = 6371.0;

    /** Velocity Model used to get slowness model. Usually set in 
     *  createSlowness(). */
    protected VelocityModel vMod;

    /** Stores the layer number for layers in the velocity model with
     *  a critical point at their top. These form the
     *  "branches" of slowness sampling.
     *  @see edu.sc.seis.TauP.CriticalDepth */
    protected Vector criticalDepthVector = new Vector();
 
    /** Stores depth ranges that contains a high
     *  slowness zone for P. Stored as DepthRange objects, containing
     *  the top depth and bottom depth. 
     *  @see DepthRange */
    protected Vector highSlownessLayerDepthsP = new Vector();
 
    /** Stores depth ranges that contains a high
     *  slowness zone for S. Stored as DepthRange objects, containing
     *  the top depth and bottom depth. 
     *  @see DepthRange */
    protected Vector highSlownessLayerDepthsS = new Vector();
 
    /** Stores depth ranges that are fluid, ie S velocity is zero.
     *  Stored as DepthRange objects, containing
     *  the top depth and bottom depth. 
     *  @see DepthRange */
    protected Vector fluidLayerDepths = new Vector();

    /** Initial length of the slowness vectors. */
    protected static int vectorLength = 256;

    /** Stores the final slowness-depth layers for P waves. Stored as 
     *  SlownessLayer objects.
     *  @see edu.sc.seis.TauP.SlownessLayer */
    protected Vector PLayers = new Vector(vectorLength);

    /** Stores the final slowness-depth layers for S waves. Stored as 
     *  SlownessLayer objects. Note that SLayers and PLayers share the
     *  same SlownessLayer object within fluid layers, so changes made
     *  to one will affect the other.
     *  @see edu.sc.seis.TauP.SlownessLayer */
    protected Vector SLayers = new Vector(vectorLength);

    /** Minimum difference between successive slowness samples. The
     *  default is 0.1 (km-sec/km or sec/rad for spherical, sec/km
     *  for flat models). This keeps the sampling from becoming too
     *  fine. For example, a strong negative S velocity gradient just
     *  above the CMB will cause the totally reflected ScS too have
     *  an extremely large range of distances, over a very small range
     *  of ray parameters. The distance check would otherwise
     *  force a very fine sampling of this region. However since in this
     *  case time and distance are likely to be very close to being 
     *  linearly related, this sort of sampling is overkill. So we
     *  ignore the distance check if the ray parameter becomes smaller
     *  than minDeltaP. */
    protected double minDeltaP = 0.1;

    /** Maximum difference between successive slowness samples. The
     *  default is 11.0 (km-sec/km or sec/rad for spherical, sec/km
     *  for flat models). See Buland and Chapman p1292 */
    protected double maxDeltaP = 11.0;

    /** Maximum difference between successive depth samples,
     *  default is 115 km. See Buland and Chapman p1292*/
    protected double maxDepthInterval = 115.0;

    /** Maximum difference between successive ranges, in radians. The
     *  default is 200 km / radiusOfEarth. See Buland and Chapman p1292.
     *  @see radiusOfEarth  */
    protected double maxRangeInterval = 200.0/radiusOfEarth;

    protected double maxInterpError = .5;

    /** Should we allow J phases, S waves in the inner core? If true, then
     *  the slowness sampling for S will use the S velocity structure for
     *  the inner core. If false, then we will use the P velocity structure
     *  for both the inner and outer core for S waves as well as P waves.
     *  Disallowing inner core S phases reduces the number of slowness
     *  samples significantly due to the large geometrical spreading
     *  of S waves in the inner core. The default is false.
     *  @see minInnerCoreDepth */
    protected boolean allowInnerCoreS = true;

    /** Tolerance for slownesses. If two slownesses are closer that this
     *  value, then we consider them to be identical. Basically this just
     *  provides some protection against numerical "chatter". */
    protected double slownessTolerance = 1e-16;

    /** Just useful for calling methods that need to know whether to use P or
     *  S waves. */
    public static final boolean PWAVE = true;

    /** Just useful for calling methods that need to know whether to use P or
     *  S waves. */
    public static final boolean SWAVE = false;

    // METHODS ----------------------------------------------------------------

    // Accessor methods

    public void setRadiusOfEarth(double radiusOfEarth) {
	this.radiusOfEarth = radiusOfEarth;
    }

    public void setMinDeltaP(double minDeltaP) {
	this.minDeltaP = minDeltaP;
    }

    public void setMaxDeltaP(double maxDeltaP) {
	this.maxDeltaP = maxDeltaP;
    }

    public void setMaxDepthInterval(double maxDepthInterval) {
	this.maxDepthInterval = maxDepthInterval;
    }

    /** sets the maximum range interval for surface focus turning
     *  waves between slowness samples, input in degrees.
     *  */
    public void setMaxRangeInterval(double maxRangeInterval) {
	this.maxRangeInterval = maxRangeInterval * Math.PI / 180.0 ;
    }

    /** sets the maximum value of the estimated error due to linear
     *  interpolation. Care should be taken not to set this too small
     *  as a very large number of samples may be required. Note also
     *  that this is only an estimate of the error, and thus the bound
     *  is by no means assured. */
    public void setMaxInterpError(double maxInterpError) {
	this.maxInterpError = maxInterpError;
    }

    public void setAllowInnerCoreS(boolean allowInnerCoreS) {
	this.allowInnerCoreS = allowInnerCoreS;
    }

    public void setSlownessTolerance(double slownessTolerance) {
	this.slownessTolerance = slownessTolerance;
    }

    // get accessor methods

    public final double getRadiusOfEarth() {
	return radiusOfEarth;
    }

    public final double getMinDeltaP() {
	return minDeltaP;
    }

    public final double getMaxDeltaP() {
	return maxDeltaP;
    }

    public final double getMaxDepthInterval() {
	return maxDepthInterval;
    }

    /** @returns the maximum range interval for surface focus turning
     *  waves between slowness samples output in degrees. */
    public final double getMaxRangeInterval() {
	return 180.0 * maxRangeInterval /Math.PI;
    }

    /** gets the maximum value of the estimated error due to linear
     *  interpolation. Care should be taken not to set this too small
     *  as a very large number of samples may be required. Note also
     *  that this is only an estimate of the error, and thus the bound
     *  is by no means assured. */
    public final double getMaxInterpError() {
	return maxInterpError;
    }

    public final boolean isAllowInnerCoreS() {
	return allowInnerCoreS;
    }

    public final double getSlownessTolerance() {
	return slownessTolerance;
    }

    public final int getNumCriticalDepths() {
	return criticalDepthVector.size();
    }

    public final CriticalDepth getCriticalDepth(int i) {
	return (CriticalDepth)criticalDepthVector.elementAt(i);
    }

    public final int getNumLayers(boolean isPWave) {
	if (isPWave) {
	    return PLayers.size();
	} else {
	    return SLayers.size();
	}
    }
   
    /** @returns the minimum ray parameter that turns, but is not reflected,
     *  at or above the given depth. Normally this is the slowness sample
     *  at the given depth, but if the depth is within a high slowness zone,
     *  then it may be smaller.
     */
    public double getMinTurnRayParam(double depth, boolean isPWave) 
	throws NoSuchLayerException, SlownessModelException {
	double minPSoFar = Double.MAX_VALUE;
	SlownessLayer sLayer;
	Vector layers;

	if (isPWave) {
	    layers = PLayers;
	} else {
	    layers = SLayers;
	}

	if (depthInHighSlowness(depth, Double.MAX_VALUE, isPWave)) {
	    for (int i=0;i<layers.size();i++) {
		sLayer = getSlownessLayer(i, isPWave);
		if (sLayer.getBotDepth() == depth) {
		    minPSoFar = Math.min(minPSoFar, sLayer.getBotP());
		    return minPSoFar;
		} else if (sLayer.getBotDepth() > depth) {
		    minPSoFar = Math.min(minPSoFar, 
					 sLayer.evaluateAt_bullen(depth,getRadiusOfEarth()));
		    return minPSoFar;
		} else {
		    minPSoFar = Math.min(minPSoFar, sLayer.getBotP());
		}
	    }
	} else {
	    sLayer = getSlownessLayer(layerNumberAbove(depth, isPWave), isPWave);
	    if (depth == sLayer.getBotDepth()) {
		minPSoFar = sLayer.getBotP();
	    } else {
		minPSoFar =  sLayer.evaluateAt_bullen(depth,getRadiusOfEarth());
	    }
	}
	return minPSoFar;
    }

    /** @returns the minimum ray parameter that turns or is reflected
     *  at or above the given depth. Normally this is the slowness sample
     *  at the given depth, but if the depth is within a high slowness zone,
     *  then it may be smaller. Also, at first order discontinuities, there
     *  may be many slowness samples at the same depth.
     */
    public double getMinRayParam(double depth, boolean isPWave) 
	throws NoSuchLayerException, SlownessModelException {
	double minPSoFar = getMinTurnRayParam(depth, isPWave); 
	int i=layerNumberAbove(depth, isPWave);
	int j=layerNumberBelow(depth, isPWave);
	SlownessLayer sLayerAbove = getSlownessLayer(i, isPWave);
	SlownessLayer sLayerBelow = getSlownessLayer(j, isPWave);

	if (sLayerAbove.getBotDepth() == depth) {
	    minPSoFar = Math.min(Math.min(minPSoFar, sLayerAbove.getBotP()), sLayerBelow.getTopP());
	}
	return minPSoFar;
    }

    /** @returns the DepthRange objects for all high slowness zones
     *  within the slowness model. */
    public DepthRange[] getHighSlowness(boolean isPWave) {
	Vector highSlownessLayerDepths;
	if (isPWave) {
	    highSlownessLayerDepths = highSlownessLayerDepthsP;
	} else {
	    highSlownessLayerDepths = highSlownessLayerDepthsS;
	}

	DepthRange[] hsz = new DepthRange[highSlownessLayerDepths.size()];
	for (int i=0; i< highSlownessLayerDepths.size();i++) {
	    hsz[i] = (DepthRange)(
				  ((DepthRange)highSlownessLayerDepths.elementAt(i)).clone());
	}
	return hsz;
    }

    /** @returns a clone of the requested waveType slowness layer. Note that 
     *  as this is a clone, no changes made to the layer will be
     *  incorporated into the slowness model. */
    public SlownessLayer getSlownessLayerClone(int layerNum, boolean isPWave) {
	if (isPWave) {
	    return (SlownessLayer)((SlownessLayer)PLayers.elementAt(layerNum)).clone();
	} else {
	    return (SlownessLayer)((SlownessLayer)SLayers.elementAt(layerNum)).clone();
	}
    }

    /** Returns the SlownessLayer of the requested waveType. This is NOT a 
     * clone and any changes will possibly corrupt the SlownessModel.
     */
    protected SlownessLayer getSlownessLayer(int layerNum, boolean isPWave) {
	if (isPWave) {
	    return (SlownessLayer)PLayers.elementAt(layerNum);
	} else {
	    return (SlownessLayer)SLayers.elementAt(layerNum);
	}
    }
   
    // Abstract methods

    public abstract double toSlowness(double velocity, double depth) throws SlownessModelException;
   
    public abstract double toVelocity(double slowness, double depth) throws SlownessModelException;
   
    public abstract TimeDist layerTimeDist(double rayParam, int layerNum, 
					   boolean isPWave) throws SlownessModelException;

    public abstract SlownessLayer toSlownessLayer(VelocityLayer vLayer, boolean isPWave) throws SlownessModelException;

    public abstract double interpolate(double p, double topVelocity,
				       double topDepth, double slope) throws SlownessModelException;

    // Defined methods

    /** generate approximate distance, in radians, for a ray from a surface
     * source that turns at the bottom of the given slowness layer. 
     *
     * @exception NoSuchLayerException occurs if no layer in the 
     *                            velocity model contains the given depth.
     * @exception SlownessModelException occurs if getNumLayers() == 0 as
     *                            we cannot compute a distance without a
     *                            layer.
     */
    public TimeDist approxDistance(int slownessTurnLayer, double p, boolean isPWave)
	throws NoSuchLayerException,
	SlownessModelException
    {
	/*
	 * First, if slowness contains less than slownessTurnLayer elements
	 * then we can't calculate a distance, otherwise we must signal
	 * an exception.
	 */
	if (slownessTurnLayer>=getNumLayers(isPWave)) {
	    throw new SlownessModelException(
					     "Can't calculate a distance when "+
					     "slownessTurnLayer >= getNumLayers("+isPWave+")\n"+
					     " slownessTurnLayer="+slownessTurnLayer+
					     " getNumLayers()="+getNumLayers(isPWave));
	}
	if (p < 0.0) {
	    throw new SlownessModelException(
					     "approxDistance: Ray parameter is negative!!!"+p+
					     " slownessTurnLayer="+slownessTurnLayer);
	}

	/*
	 * OK, now we are able to do the calculations for the approximate
	 * distance, hopefully without errors.
	 */

	TimeDist td = new TimeDist(p);
	TimeDist layerTD;

	for (int layerNum=0;layerNum<=slownessTurnLayer;layerNum++) {
	    td.add( layerTimeDist(p, layerNum, isPWave));
	}
	/* Return 2.0*distance and time because there is a downgoing as well as
	 * up going leg, which are equal because this is for a surface source.
	 */
	td.dist *= 2.0;
	td.time *= 2.0;
	return td;
    }

    /** Determines if the given depth and corresponding slowness
     *  is contained within a high
     *  slowness zone. Whether the high slowness zone includes its upper
     *  boundary and its lower boundaries depends upon the ray parameter.
     *  The slowness at the depth is needed because if depth happens to
     *  correspond to a discontinuity that marks the bottom of the
     *  high slowness zone but the ray is actually a total reflection
     *  then it is not part of the high slowness zone.
     *  Calls depthInHighSlowness(double, double, DepthRange). 
     *  @see depthInHighSlowness. */
    public boolean depthInHighSlowness(double depth, double rayParam, boolean isPWave) {
	DepthRange highSZoneDepth = new DepthRange();
	return depthInHighSlowness(depth, rayParam, highSZoneDepth, isPWave);
    }

    /** Determines if the given depth and corresponding slowness
     * is contained within a high
     *  slowness zone. Whether the high slowness zone includes its upper
     *  boundary and its lower boundaries depends upon the ray parameter.
     *  The slowness at the depth is needed because if depth happens to
     *  correspond to a discontinuity that marks the bottom of the
     *  high slowness zone but the ray is actually a total reflection
     *  then it is not part of the high slowness zone.
     *  The ray parameter that delimits the zone, ie it can turn at the
     *  top and the bottom, is in the zone at the top, but out of the
     *  zone at the bottom.
     *  */
    public boolean depthInHighSlowness(double depth, double rayParam,
				       DepthRange highSZoneDepth, boolean isPWave) {
	DepthRange tempRange;
	Vector highSlownessLayerDepths;
	if (isPWave) {
	    highSlownessLayerDepths = highSlownessLayerDepthsP;
	} else {
	    highSlownessLayerDepths = highSlownessLayerDepthsS;
	}

	for (int i=0;i<highSlownessLayerDepths.size();i++) {
	    tempRange = (DepthRange)highSlownessLayerDepths.elementAt(i);
	    if (tempRange.topDepth <= depth && depth <= tempRange.botDepth) {
		highSZoneDepth.topDepth = tempRange.topDepth;
		highSZoneDepth.botDepth = tempRange.botDepth;
		highSZoneDepth.rayParam = tempRange.rayParam;
		if (rayParam > tempRange.rayParam || (
						      rayParam == tempRange.rayParam &&
						      depth == tempRange.topDepth)) {
		    return true;
		}
	    }
	}
	return false;
    }

    /** Determines if the given depth is contained within a fluid
     *  zone. The fluid zone includes its upper
     *  boundary but not its lower boundary. Calls
     *  depthInFluid(double, DepthRange).
     *  @see depthInFluid(double, DepthRange). */
    public boolean depthInFluid(double depth) {
	DepthRange fluidZoneDepth = new DepthRange();
	return depthInFluid(depth, fluidZoneDepth);
    }
 
    /** Determines if the given depth is contained within a 
     *  fluid zone. The fluid zone includes its upper
     *  boundary but not its lower boundary. The top and bottom
     *  of the fluid zone are returned in DepthRange.
     */
    public boolean depthInFluid(double depth, DepthRange fluidZoneDepth) {
	DepthRange tempRange;
 
	for (int i=0;i<fluidLayerDepths.size();i++) {
	    tempRange = (DepthRange)fluidLayerDepths.elementAt(i);
	    if (tempRange.topDepth <= depth && depth < tempRange.botDepth) {
		fluidZoneDepth.topDepth = tempRange.topDepth;
		fluidZoneDepth.botDepth = tempRange.botDepth;
		return true;
	    }
	}
	return false;
    }

    /* Splits a slowness layer into two slowness layers. 
       returns a SplitLayerInfo object with 
       neededSplit=true if a layer was actually split, false otherwise,
       movedSample=true if a layer was very close, and so moving the layers
       depth is better than making a very thin layer,
       rayParam= the new ray parameter, if the layer was split.
       The interpolation for splitting a layer
       is a Bullen p=Ar^B and so does not directly use information from the
       VelocityModel. */
    public SplitLayerInfo splitLayer(double depth, boolean isPWave) 
	throws SlownessModelException, NoSuchLayerException {
	Vector layers, otherLayers;
	if (isPWave) {
	    layers = PLayers;
	    otherLayers = SLayers;
	} else {
	    layers = SLayers;
	    otherLayers = PLayers;
	}
	boolean otherWaveType = ! isPWave;
	boolean changeMade = false;

	int layerNum = layerNumberAbove(depth, isPWave);
	SlownessLayer sLayer = getSlownessLayer(layerNum, isPWave);

	if (sLayer.getTopDepth()==depth || sLayer.getBotDepth()==depth) {
            /* depth is already on a slowness layer boundary so we don't
             * need to split any slowness layers. */
	    return new SplitLayerInfo(false, false, 0.0);
	} else if (Math.abs(sLayer.getTopDepth() - depth) < 0.000001) {
	    /* check for very thin layers, just move the layer to hit the boundary */
	    sLayer.setTopDepth(depth);
	    sLayer = getSlownessLayer(layerNum-1, isPWave);
	    sLayer.setBotDepth(depth);
	    return new SplitLayerInfo(false, true, sLayer.getBotP());
	} else if (Math.abs(depth - sLayer.getBotDepth()) < 0.000001) {
	    /* check for very thin layers, just move the layer to hit the boundary */
	    sLayer.setBotDepth(depth);
	    sLayer = getSlownessLayer(layerNum+1, isPWave);
	    sLayer.setTopDepth(depth);
	    return new SplitLayerInfo(false, true, sLayer.getTopP());
	} else {
	    double p = sLayer.evaluateAt_bullen(depth, radiusOfEarth);
	    int index = -1;
	    int otherIndex = -1;
	    SlownessLayer topLayer, botLayer;
	  
	    topLayer = (SlownessLayer)sLayer.clone();
	    topLayer.setBotP(p);
	    topLayer.setBotDepth(depth);

	    botLayer = (SlownessLayer)sLayer.clone();
	    botLayer.setTopP(p);
	    botLayer.setTopDepth(topLayer.getBotDepth());
	    layers.removeElementAt(layerNum);
	    layers.insertElementAt(botLayer, layerNum);
	    layers.insertElementAt(topLayer, layerNum);
	    // fix critical layers since we have added a slowness layer
	    for (int i=0; i<getNumCriticalDepths(); i++) {
		CriticalDepth cd = getCriticalDepth(i);
		if (cd.getLayerNum(isPWave) > layerNum) {
		    cd.setLayerNum(cd.getLayerNum(isPWave)+1, isPWave);
		}
	    }
	    // now make sure we keep the sampling consistant
	    // if in a fluid, then both wavetypes will share a single 
	    // slowness layer object. Otherwise indexOf returns -1
	    otherIndex = otherLayers.indexOf(sLayer);
	    if (otherIndex != -1) {
		otherLayers.removeElementAt(otherIndex);
		otherLayers.insertElementAt(botLayer, otherIndex);
		otherLayers.insertElementAt(topLayer, otherIndex);
	    }
	    for (int otherLayerNum=0; otherLayerNum< otherLayers.size() ; otherLayerNum++) {
		sLayer = (SlownessLayer)otherLayers.elementAt(otherLayerNum);
		if ((sLayer.getTopP() - p)*(p-sLayer.getBotP()) > 0.0) {
		    // found the slowness layer with the other wave type that
		    // contains the new slowness sample
		    topLayer = (SlownessLayer)sLayer.clone();
		    topLayer.setBotP(p);
		    topLayer.setBotDepth(sLayer.bullenDepthFor(p, radiusOfEarth));
		  
		    botLayer = (SlownessLayer)sLayer.clone();
		    botLayer.setTopP(p);
		    botLayer.setTopDepth(topLayer.getBotDepth());
		  
		    otherLayers.removeElementAt(otherLayerNum);
		    otherLayers.insertElementAt(botLayer, otherLayerNum);
		    otherLayers.insertElementAt(topLayer, otherLayerNum);
		    // fix critical layers since we have added a slowness layer
		    for (int critNum=0; critNum<getNumCriticalDepths(); critNum++) {
			CriticalDepth cd = getCriticalDepth(critNum);
			if (cd.getLayerNum(otherWaveType) > otherLayerNum) {
			    cd.setLayerNum(cd.getLayerNum(otherWaveType)+1, otherWaveType);
			}
		    }
		}
	    }
	    return new SplitLayerInfo(true, false, p);
	}
    }

    /**
     * Finds all critical points within a velocity model. Critical points
     * are first order discontinuities in velocity/slowness, local extrema
     * in slowness. A high
     * slowness zone is a low velocity zone, but it is possible to have
     * a slight low velocity zone within a spherical earth that is
     * not a high slowness zone and thus does not exhibit 
     * any of the pathological behavior of a low velocity zone.
     *
     * @exception NoSuchMatPropException occurs if wavetype is not
     *    recognized.
     * @exception SlownessModelException occurs if validate() returns
     *    false, this indicates a bug in the code.
     */
    protected void findCriticalPoints()
	throws SlownessModelException {
	
	double topP, botP;
	double topDepth, botDepth = getRadiusOfEarth();
	double topVelocity, botVelocity;
	
	double minPSoFar = Double.MAX_VALUE;
	double minSSoFar = Double.MAX_VALUE;
	DepthRange highSlownessZoneP = new DepthRange();
	DepthRange highSlownessZoneS = new DepthRange();
	boolean inHighSlownessZoneP = false;
	boolean inHighSlownessZoneS = false;
	
	DepthRange fluidZone = new DepthRange();
	boolean inFluidZone = false;
	
	boolean belowOuterCore = false;  /* are we in the inner core,
					  * see allowInnerCoreS. */
	
	VelocityLayer prevVLayer, currVLayer;
	
	SlownessLayer prevSLayer, currSLayer, prevPLayer, currPLayer;
	
	// First remove any critical points previously stored
	highSlownessLayerDepthsP.removeAllElements();
	highSlownessLayerDepthsS.removeAllElements();
	criticalDepthVector.removeAllElements();
	fluidLayerDepths.removeAllElements();
	
	// Initialize the current velocity layer
	// to be zero thickness layer with values at the surface
    currVLayer = vMod.getVelocityLayer(0);
    currVLayer = new VelocityLayer(0,
                                   currVLayer.getTopDepth(), currVLayer.getTopDepth(),
                                   currVLayer.getTopPVelocity(), currVLayer.getTopPVelocity(),
                                   currVLayer.getTopSVelocity(), currVLayer.getTopSVelocity(),
                                   currVLayer.getTopDensity(), currVLayer.getTopDensity(),
                                   currVLayer.getTopQp(), currVLayer.getTopQp(),
                                   currVLayer.getTopQs(),currVLayer.getTopQs());
	
	currSLayer =  toSlownessLayer(currVLayer, SWAVE);
	currPLayer =  toSlownessLayer(currVLayer, PWAVE);
	
	
	// We know that the top is always a critical slowness so add 0
	criticalDepthVector.addElement(new CriticalDepth(0.0, 0, 0, 0));
	
	/* Check to see if we start in a fluid zone. */
	if (!inFluidZone && currVLayer.getTopSVelocity()==0.0 )  {
	    inFluidZone = true;
	    fluidZone = new DepthRange();
	    fluidZone.topDepth = currVLayer.getTopDepth();
	    currSLayer = currPLayer;
	}
	if (minSSoFar > currSLayer.getTopP()) {
	    minSSoFar = currSLayer.getTopP();
	}
	if (minPSoFar > currPLayer.getTopP()) {
	    minPSoFar = currPLayer.getTopP();
	}
	
	for (int layerNum=0;layerNum<vMod.getNumLayers();layerNum++) {
	    
	    prevVLayer = currVLayer;
	    prevSLayer = currSLayer;
	    prevPLayer = currPLayer;
	    currVLayer = vMod.getVelocityLayerClone(layerNum);
	    currSLayer = toSlownessLayer(currVLayer, SWAVE);
	    currPLayer = toSlownessLayer(currVLayer, PWAVE);

            /* If we are not already in a fluid check to see if we have 
             * just entered a fluid zone. */
	    if (!inFluidZone && currVLayer.getTopSVelocity()==0.0 )  {
		inFluidZone = true;
		fluidZone = new DepthRange();
		fluidZone.topDepth = currVLayer.getTopDepth();
	    }

            /* If we are already in a fluid check to see if we have 
             * just exited it. */
	    if (inFluidZone && currVLayer.getTopSVelocity()!=0.0)  {
		if (prevVLayer.getBotDepth() > vMod.getIocbDepth() ) {
		    belowOuterCore = true;
		}
		inFluidZone = false;
		fluidZone.botDepth = prevVLayer.getBotDepth();
		fluidLayerDepths.addElement(fluidZone);
	    }

            /* If we are in a fluid zone ( S velocity = 0.0 ) or if
             * we are below the outer core and allowInnerCoreS=false
             * then use the P velocity structure to look for critical points. */
	    if (inFluidZone || ( belowOuterCore && ! allowInnerCoreS )) {
		currSLayer = currPLayer;
	    }

	    if (prevSLayer.getBotP() != currSLayer.getTopP() || prevPLayer.getBotP() != currPLayer.getTopP()) {
		// first order discontinuity
		criticalDepthVector.addElement(new CriticalDepth(currSLayer.getTopDepth(), layerNum, -1, -1));

		if (DEBUG) {
		    System.out.println("first order discontinuity, depth="+
				       currSLayer.getTopDepth());
		    System.out.println(prevSLayer+"\n"+currSLayer);
		    System.out.println(prevPLayer+"\n"+currPLayer);
		}

		if (inHighSlownessZoneS && (currSLayer.getTopP() < minSSoFar)) {
		    // top of current layer is the bottom of a high slowness zone.
		    if (DEBUG) {
			System.out.println("top of current layer is the bottom"+
					   " of a high slowness zone.");
		    }
		    highSlownessZoneS.botDepth = currSLayer.getTopDepth();
		    highSlownessLayerDepthsS.addElement(highSlownessZoneS);
		    inHighSlownessZoneS = false;
		}
		if (inHighSlownessZoneP && (currPLayer.getTopP() < minPSoFar)) {
		    // top of current layer is the bottom of a high slowness zone.
		    if (DEBUG) {
			System.out.println("top of current layer is the bottom"+
					   " of a high slowness zone.");
		    }
		    highSlownessZoneP.botDepth = currSLayer.getTopDepth();
		    highSlownessLayerDepthsP.addElement(highSlownessZoneP);
		    inHighSlownessZoneP = false;
		}

		/* Update minPSoFar and minSSoFar as all total reflections off of the top
		 * of the discontinuity are ok even though below the 
		 * discontinuity could be the start of a high slowness zone. */
		if (minPSoFar > currPLayer.getTopP()) {
		    minPSoFar = currPLayer.getTopP();
		}
		if (minSSoFar > currSLayer.getTopP()) {
		    minSSoFar = currSLayer.getTopP();
		}
            
		if (!inHighSlownessZoneS && (
					     prevSLayer.getBotP() < currSLayer.getTopP() ||
					     currSLayer.getTopP() < currSLayer.getBotP())) {
		    // start of a high slowness zone
		    if (DEBUG) {
			System.out.println("Found S high slowness at first order "+
					   "discontinuity, layer = "+layerNum);
		    }
		    inHighSlownessZoneS = true;
		    highSlownessZoneS = new DepthRange();
		    highSlownessZoneS.topDepth = currSLayer.getTopDepth();
		    highSlownessZoneS.rayParam = minSSoFar;
		}
		if (!inHighSlownessZoneP && (
					     prevPLayer.getBotP() < currPLayer.getTopP() ||
					     currPLayer.getTopP() < currPLayer.getBotP())) {
		    // start of a high slowness zone
		    if (DEBUG) {
			System.out.println("Found P high slowness at first order "+
					   "discontinuity, layer = "+layerNum);
		    }
		    inHighSlownessZoneP = true;
		    highSlownessZoneP = new DepthRange();
		    highSlownessZoneP.topDepth = currPLayer.getTopDepth();
		    highSlownessZoneP.rayParam = minPSoFar;
		}

	    } else {
		if ((prevSLayer.getTopP()-prevSLayer.getBotP())*
		    (prevSLayer.getBotP()-currSLayer.getBotP()) < 0.0 ||
		    (prevPLayer.getTopP()-prevPLayer.getBotP())*
		    (prevPLayer.getBotP()-currPLayer.getBotP()) < 0.0) {
		         	// local slowness extrema
		    criticalDepthVector.addElement(new CriticalDepth(currSLayer.getTopDepth(),layerNum,-1,-1));

		    if (DEBUG) {
			System.out.println("local slowness extrema, depth="+
					   currSLayer.getTopDepth());
		    }

		    if (!inHighSlownessZoneP && (currPLayer.getTopP() < currPLayer.getBotP())) {
			// start of a high slowness zone
			if (DEBUG) {
			    System.out.println("start of a P high slowness zone,"+
					       " local slowness extrema, minPSoFar="+minPSoFar);
			}
			inHighSlownessZoneP = true;
			highSlownessZoneP = new DepthRange();
			highSlownessZoneP.topDepth = currPLayer.getTopDepth();
			highSlownessZoneP.rayParam = minPSoFar;
		    }
		    if (!inHighSlownessZoneS && (currSLayer.getTopP() < currSLayer.getBotP())) {
			// start of a high slowness zone
			if (DEBUG) {
			    System.out.println("start of a S high slowness zone,"+
					       " local slowness extrema, minSSoFar="+minSSoFar);
			}
			inHighSlownessZoneS = true;
			highSlownessZoneS = new DepthRange();
			highSlownessZoneS.topDepth = currSLayer.getTopDepth();
			highSlownessZoneS.rayParam = minSSoFar;
		    }
		}
	    }

	    if (inHighSlownessZoneP && (currPLayer.getBotP() < minPSoFar)) {
		// layer contains the bottom of a high slowness zone.
		if (DEBUG) {
		    System.out.println("layer contains the bottom of a P "+
				       "high slowness zone. minPSoFar="+minPSoFar+" "+currPLayer);
		}
		highSlownessZoneP.botDepth = findDepth(minPSoFar,
						       layerNum, layerNum, PWAVE);
		highSlownessLayerDepthsP.addElement(highSlownessZoneP);
		inHighSlownessZoneP = false;
	    }
	    if (inHighSlownessZoneS && (currSLayer.getBotP() < minSSoFar)) {
		// layer contains the bottom of a high slowness zone.
		if (DEBUG) {
		    System.out.println("layer contains the bottom of a S "+
				       "high slowness zone. minSSoFar="+minSSoFar+" "+currSLayer);
		}
		highSlownessZoneS.botDepth = findDepth(minSSoFar,
						       layerNum, layerNum, SWAVE);
		highSlownessLayerDepthsS.addElement(highSlownessZoneS);
		inHighSlownessZoneS = false;
	    }

	    if (minPSoFar > currPLayer.getBotP()) {
		minPSoFar = currPLayer.getBotP();
	    }
	    if (minPSoFar > currPLayer.getTopP()) {
		minPSoFar = currPLayer.getTopP();
	    }
	    if (minSSoFar > currSLayer.getBotP()) {
		minSSoFar = currSLayer.getBotP();
	    }
	    if (minSSoFar > currSLayer.getTopP()) {
		minSSoFar = currSLayer.getTopP();
	    }

	    if (DEBUG && inHighSlownessZoneS) {
		System.out.println("In S high slowness zone, layerNum = "+layerNum+
				   " minSSoFar="+minSSoFar);
	    }
	    if (DEBUG && inHighSlownessZoneP) {
		System.out.println("In P high slowness zone, layerNum = "+layerNum+
				   " minPSoFar="+minPSoFar);
	    }
	}

	// We know that the bottommost depth is always a critical slowness,
	// so we add vMod.getNumLayers()
	criticalDepthVector.addElement(new CriticalDepth(getRadiusOfEarth(),vMod.getNumLayers(),-1,-1));

	// Check if the bottommost depth is contained within a high slowness
	// zone, might happen in a flat non-whole-earth model
	if (inHighSlownessZoneS) {
	    highSlownessZoneS.botDepth = currVLayer.getBotDepth();
	    highSlownessLayerDepthsS.addElement(highSlownessZoneS);
	}
	if (inHighSlownessZoneP) {
	    highSlownessZoneP.botDepth = currVLayer.getBotDepth();
	    highSlownessLayerDepthsP.addElement(highSlownessZoneP);
	}

	/* Check if the bottommost depth is contained within a fluid zone,
	 * this would be the case if we have a non whole earth model with
	 * the bottom in the outer core or if allowInnerCoreS == false
	 * and we want to use the P velocity structure in the inner core. */
	if (inFluidZone ) {
	    fluidZone.botDepth = currVLayer.getBotDepth();
	    fluidLayerDepths.addElement(fluidZone);
	}

	if (DEBUG && criticalDepthVector.size()!=0) {
	    int botCriticalLayerNum, topCriticalLayerNum;
	    String desc = 
		"**** Critical Velocity Layers ************************\n";
	    botCriticalLayerNum = 
		((CriticalDepth)criticalDepthVector.elementAt(0)).velLayerNum-1;
	    for (int criticalNum=1; criticalNum<criticalDepthVector.size(); criticalNum++) {
		topCriticalLayerNum = botCriticalLayerNum+1;
		botCriticalLayerNum = 
		    ((CriticalDepth)criticalDepthVector.elementAt(criticalNum)).velLayerNum-1;
		desc += " "+topCriticalLayerNum+","+botCriticalLayerNum;
	    }
	    System.out.println(desc);
	}

	if (DEBUG && highSlownessLayerDepthsP.size()!=0) {
	    for (int layerNum=0;layerNum<highSlownessLayerDepthsP.size();layerNum++) {
		System.out.println((DepthRange)highSlownessLayerDepthsP.elementAt(layerNum));
	    }
	}
	if (DEBUG && highSlownessLayerDepthsS.size()!=0) {
	    for (int layerNum=0;layerNum<highSlownessLayerDepthsS.size();layerNum++) {
		System.out.println((DepthRange)highSlownessLayerDepthsS.elementAt(layerNum));
	    }
	}
      
	if (!validate()) {
	    throw new SlownessModelException("Validation Failed!");
	}
    }
   
    /** Finds a depth corresponding to a slowness over the whole 
     *  VelocityModel. Calls findDepth(double, int, int, char).
     */
    public double findDepth(double rayParam, boolean isPWave) 
	throws SlownessModelException {
	return findDepth(rayParam, 0, vMod.getNumLayers()-1, isPWave);
    }

    /** Finds a depth corresponding to a slowness between two given
     *  depths in the Velocity Model. Calls findDepth(double, int, int, char).
     */
    public double findDepth(double rayParam, double topDepth, double botDepth,
			    boolean isPWave) throws SlownessModelException {
	try {
	    int topLayerNum = vMod.layerNumberBelow(topDepth);
	    if (vMod.getVelocityLayer(topLayerNum).getBotDepth() == topDepth) {
		topLayerNum++;
	    }
	    int botLayerNum = vMod.layerNumberAbove(botDepth);
	    return findDepth(rayParam, topLayerNum, botLayerNum, isPWave);
	} catch (NoSuchLayerException e) {
	    throw new SlownessModelException(e.getMessage());
	}
    }

    /** Finds a depth corresponding to a slowness between two given 
     *  velocity layers, including the top and the bottom.
     *  We also check to see if the slowness is less than the bottom
     *  slowness of these layers but greater than the top slowness
     *  of the next deeper layer. This corresponds to a total reflection.
     *  In this case a check needs to be made to see if this is an S wave
     *  reflecting off of a fluid layer, use P velocity below in this case.
     *  We assume that slowness is monotonic within these layers and
     *  therefore there is only one depth with the given slowness.
     *  This means we return the first depth that we find.
     *
     *  @exception SlownessModelException occurs if
     *     topCriticalLayer > botCriticalLayer because there are no layers 
     *     to search, or if there is an increase in slowness, ie a negative
     *     velocity gradient, that just balances the decrease in slowness due
     *     to the spherical earth, or if the ray parameter p is not contained
     *     within the specified layer range.
     */
    public double findDepth(double p,
			    int topCriticalLayer, int botCriticalLayer, boolean isPWave)
	throws SlownessModelException {

	VelocityLayer velLayer = new VelocityLayer();
	double topP=Double.MAX_VALUE, botP=Double.MAX_VALUE;
	double topVelocity, botVelocity;
	double depth;
	double denominator;
	double slope;

	char waveType;
	if (isPWave) {
	    waveType = 'P';
	} else {
	    waveType = 'S';
	}
		
	try {
	    if (topCriticalLayer > botCriticalLayer) {
		throw new SlownessModelException("findDepth: no layers to search!: "+
						 "topCriticalLayer = "+topCriticalLayer+
						 "botCriticalLayer = "+botCriticalLayer);
	    }

	    for (int layerNum=topCriticalLayer;layerNum<=botCriticalLayer;layerNum++){
		velLayer = (VelocityLayer)vMod.getVelocityLayer(layerNum);
		topVelocity = velLayer.evaluateAtTop(waveType);
		botVelocity = velLayer.evaluateAtBottom(waveType);

		topP = toSlowness(topVelocity, velLayer.getTopDepth());
		botP = toSlowness(botVelocity, velLayer.getBotDepth());
	 
		/* check to see if we are within chatter level of the top or bottom
		 * and if so then return that depth. */
		if (Math.abs(topP - p) < slownessTolerance) {
	            return velLayer.getTopDepth();
		} 
		if (Math.abs(p - botP) < slownessTolerance) {
	            return velLayer.getBotDepth();
		} 

		if ((topP - p)*(p - botP) >= 0.0) {  // found the layer containing p
		    /*
		     * We interpolate assuming that velocity is
		     * linear within this interval. So slope is the slope
		     * for velocity versus depth.
		     */
	            slope = (botVelocity-topVelocity)/
			(velLayer.getBotDepth()-velLayer.getTopDepth());
	            depth = interpolate(p, topVelocity, velLayer.getTopDepth(), slope);
	            return depth;

		} else if (layerNum==topCriticalLayer && 
			   Math.abs(p-topP)<slownessTolerance) {
		    /* Check to see if p is just outside the topmost layer.
		     * If so than return the top depth.  */
	            return velLayer.getTopDepth();
		}

		/* Is p a total reflection? 
		 * botP is the slowness at the bottom of the last velocity
		 * layer from the previous loop, set topP to be the slowness
		 * at the top of the next layer. */
		if (layerNum < vMod.getNumLayers()-1) {
	            velLayer = (VelocityLayer)vMod.getVelocityLayerClone(
									 layerNum+1);
	            topVelocity = velLayer.evaluateAtTop(waveType);
	   
	            if (! isPWave &&
			depthInFluid(velLayer.getTopDepth())){
			/* Special case for S waves above a fluid. If 
			 * top next layer is in a fluid then we should set topVelocity
			 * to be the P velocity at the top of the layer. */
			topVelocity = velLayer.evaluateAtTop('P');
	            } 
	            topP = toSlowness(topVelocity, velLayer.getTopDepth());
	            if (botP >= p && p >= topP) {
	                return velLayer.getTopDepth();
	            }
		}
	    }

	    if (Math.abs(p-botP)<slownessTolerance) {
		/* Check to see if p is just outside the bottommost layer.
		 * If so than return the bottom depth.  */
		System.out.println(" p is just outside the bottommost layer."+
				   " This probably shouldn't be allowed to happen!\n");
		return velLayer.getBotDepth();
	    }
	} catch (NoSuchMatPropException e) {
	    // can't happen...
	    e.printStackTrace();
	}
		
	throw new SlownessModelException("slowness p="+p+
					 " is not contained within the specified layers."+
					 "\np="+p+" topCriticalLayer="+topCriticalLayer+
					 " botCriticalLayer="+ botCriticalLayer+" isPWave="+ isPWave+
					 " topP="+topP+" botP="+botP);
    }

    /**
     * This method takes a velocity model and creates a vector containing
     * slowness-depth layers that, hopefully, adequately sample both
     * slowness and depth so that the travel time as a function of
     * distance can be reconstructed from the theta function.
     * It catches NoSuchLayerException which might be generated in the 
     * velocity model. This shouldn't happen though.
     * @see VelocityModel
     * @exception SlownessModelException occurs if the validation on the
     *    velocity model fails, or if the velocity model
     *    has no layers.
     * @exception NoSuchMatPropException occurs if wavetype is not
     *    recognized.
     */
    public void createSample(VelocityModel velModel) 
	throws SlownessModelException, NoSuchMatPropException, NoSuchLayerException
    {

	VelocityLayer velLayer;
	double maxVelSoFar = 0.0;
	double previousBotVel;
	int botCriticalLayerNum, topCriticalLayerNum;
	DepthRange highSZoneDepth = new DepthRange();

      
	// First check to make sure velocity model is ok.
	if (velModel.validate()== false) {
	    throw new SlownessModelException("Error in velocity model!");
	}
	if (velModel.getNumLayers()==0) {
	    throw new SlownessModelException("velModel.getNumLayers()==0");
	}
	      
	if (DEBUG) {System.out.println("start createSample");}
	vMod = velModel;
	setRadiusOfEarth(velModel.getRadiusOfEarth());

 
	if (DEBUG) {System.out.println("findCriticalPoints");}
	findCriticalPoints();
	if (DEBUG) {System.out.println("coarseSample");}
	coarseSample();

	boolean isOK = false;
	if (DEBUG) {
	    isOK = validate();
	    System.out.println("rayParamCheck");}
	rayParamIncCheck();

	if (DEBUG) {
	    isOK &= validate();
	    System.out.println("depthIncCheck");}
	depthIncCheck();

	if (DEBUG) {
	    isOK &= validate();
	    System.out.println("distanceCheck");}
	distanceCheck();

	if (DEBUG) {
	    isOK &= validate();
	    System.out.println("fixCriticalPoints");}
	fixCriticalPoints();

	if (DEBUG) {
	    System.out.println("done createSample");
	}
    }
   
    /** Creates a coarse slowness sampling of the velocity model (vMod). The
     *  resultant slowness layers will satisfy the maximum depth increments
     *  as well as sampling each point specified within the VelocityModel. 
     * The P and S sampling will also be compatible.
     */
    protected void coarseSample() 
	throws SlownessModelException, NoSuchLayerException {
	VelocityLayer prevVLayer;
	VelocityLayer origVLayer;
	VelocityLayer currVLayer = new VelocityLayer();
	SlownessLayer currPLayer, currSLayer;

	PLayers.removeAllElements();
	SLayers.removeAllElements();
      
      	// to initialize prevVLayer
    origVLayer = vMod.getVelocityLayer(0);
    origVLayer = new VelocityLayer(0, 
                                   origVLayer.getTopDepth(), origVLayer.getTopDepth(),
                                   origVLayer.getTopPVelocity(), origVLayer.getTopPVelocity(),
                                   origVLayer.getTopSVelocity(), origVLayer.getTopSVelocity(),
                                   origVLayer.getTopDensity(), origVLayer.getTopDensity(),
                                   origVLayer.getTopQp(), origVLayer.getTopQp(),
                                   origVLayer.getTopQs(),origVLayer.getTopQs());

	try {
	    for (int layerNum=0; layerNum < vMod.getNumLayers(); layerNum++) {
	      	prevVLayer = origVLayer;
		origVLayer = vMod.getVelocityLayer(layerNum);
	         
		/* Check for first order discontinuity. However, we only 
		 * consider S discontinuities in the inner core if 
		 * allowInnerCoreS is true. */
		if (prevVLayer.getBotPVelocity() != origVLayer.getTopPVelocity() ||
		    (prevVLayer.getBotSVelocity() != origVLayer.getTopSVelocity() &&
		     (allowInnerCoreS || 
		      origVLayer.getTopDepth() < vMod.getIocbDepth()))) {
		    currVLayer.setTopDepth(prevVLayer.getBotDepth());
		    currVLayer.setBotDepth(prevVLayer.getBotDepth());
		    currVLayer.setTopPVelocity(prevVLayer.evaluateAtBottom('P'));
		    currVLayer.setBotPVelocity(origVLayer.evaluateAtTop('P'));
	         	
		    /* if we are going from a fluid to a solid or
                       solid to fluid, ex core mantle or outer core to
                       inner core then we need to use the P velocity
                       for determining the S discontinuity. */
		    if (prevVLayer.getBotSVelocity() == 0.0) {
			currVLayer.setTopSVelocity(prevVLayer.evaluateAtBottom('P'));
		    } else {
			currVLayer.setTopSVelocity(prevVLayer.evaluateAtBottom('S'));
		    }
		    if (origVLayer.getTopSVelocity() == 0.0) {
			currVLayer.setBotSVelocity(origVLayer.evaluateAtTop('P'));
		    } else {
			currVLayer.setBotSVelocity(origVLayer.evaluateAtTop('S'));
		    }
	         	
		    /* Add the zero thickness, but with nonzero
                       slowness step, layer corresponding to the
                       discontinuity. */
		    currPLayer = toSlownessLayer(currVLayer, PWAVE);
		    PLayers.addElement(currPLayer);
		    if ((prevVLayer.getBotSVelocity() == 0.0 && 
			 origVLayer.getTopSVelocity() == 0.0) || 
			(! allowInnerCoreS && 
			 currVLayer.getTopDepth() >= vMod.getIocbDepth())) {
			currSLayer = currPLayer;
		    } else {
			currSLayer = toSlownessLayer(currVLayer, SWAVE);
		    }
		    SLayers.addElement(currSLayer);
		}
	         
		currPLayer = toSlownessLayer(origVLayer, PWAVE);
		PLayers.addElement(currPLayer);
	         
		if (depthInFluid(origVLayer.getTopDepth()) || 
		    (! allowInnerCoreS && origVLayer.getTopDepth() >= vMod.getIocbDepth())) {
		    currSLayer = currPLayer;
		} else {
		    currSLayer = toSlownessLayer(origVLayer, SWAVE);
		}
		SLayers.addElement(currSLayer);
	    }
			
			
	    // make sure that all high slowness layers are sampled exactly
	    // at their bottom
	    int highZoneNum, SLayerNum;
	    SlownessLayer highSLayer;
	    DepthRange highZone;
	    for (highZoneNum = 0; highZoneNum < highSlownessLayerDepthsS.size(); highZoneNum++) {
		highZone = (DepthRange)highSlownessLayerDepthsS.elementAt(highZoneNum);
		SLayerNum = layerNumberAbove(highZone.botDepth, SWAVE);
		highSLayer = getSlownessLayer(SLayerNum, SWAVE);
		while (highSLayer.getTopDepth() == highSLayer.getBotDepth() && 
		       (highSLayer.getTopP() - highZone.rayParam)*(highZone.rayParam - highSLayer.getBotP()) < 0) {
		    SLayerNum++;
		    highSLayer = getSlownessLayer(SLayerNum, SWAVE);
		}
		if (highZone.rayParam != highSLayer.getBotP()) {
		    addSlowness(highZone.rayParam, SWAVE);
		}
	    }
	    for (highZoneNum = 0; highZoneNum < highSlownessLayerDepthsP.size(); highZoneNum++) {
		highZone = (DepthRange)highSlownessLayerDepthsP.elementAt(highZoneNum);
		SLayerNum = layerNumberAbove(highZone.botDepth, PWAVE);
		highSLayer = getSlownessLayer(SLayerNum, PWAVE);
		while (highSLayer.getTopDepth() == highSLayer.getBotDepth() && 
		       (highSLayer.getTopP() - highZone.rayParam)*(highZone.rayParam - highSLayer.getBotP()) < 0) {
		    SLayerNum++;
		    highSLayer = getSlownessLayer(SLayerNum, PWAVE);
		}
		if (highZone.rayParam != highSLayer.getBotP()) {
		    addSlowness(highZone.rayParam, PWAVE);
		}
	    }
	    // make sure P and S sampling are consistant
	    double botP = -1;
	    double topP = -1;
	    for (int j=0; j<PLayers.size(); j++) {
		topP = ((SlownessLayer)PLayers.elementAt(j)).getTopP();
		if (topP != botP) {
		    addSlowness(topP, SWAVE);
		}
		botP = ((SlownessLayer)PLayers.elementAt(j)).getBotP();
		addSlowness(botP, SWAVE);
	    }
	    botP = -1;
	    for (int j=0; j<SLayers.size(); j++) {
		topP = ((SlownessLayer)SLayers.elementAt(j)).getTopP();
		if (topP != botP) {
		    addSlowness(topP, PWAVE);
		}
		botP = ((SlownessLayer)SLayers.elementAt(j)).getBotP();
		addSlowness(botP, PWAVE);
	    }
	} catch (NoSuchMatPropException e) {
	    // can't happen...
	    e.printStackTrace();
	}
    }
   
    /** Checks to make sure that no slowness layer spans more than maxDeltaP. 
     */
    protected void rayParamIncCheck() 
	throws SlownessModelException, NoSuchLayerException 
    {
   	SlownessLayer sLayer;
   	double numNewP;
   	double deltaP;

   	for (int j=0; j<SLayers.size(); j++) {
	    sLayer = (SlownessLayer)SLayers.elementAt(j);
	    if (Math.abs(sLayer.getTopP() - sLayer.getBotP()) > maxDeltaP) {
		numNewP = Math.ceil(Math.abs(sLayer.getTopP() - sLayer.getBotP()) 
				    / maxDeltaP);
		deltaP = (sLayer.getTopP() - sLayer.getBotP()) / numNewP;

		for (int rayNum=1; rayNum < numNewP; rayNum++) {
		    addSlowness(sLayer.getTopP()+rayNum*deltaP, PWAVE);
		    addSlowness(sLayer.getTopP()+rayNum*deltaP, SWAVE);
		}
	    }
	}
   	for (int j=0; j<PLayers.size(); j++) {
	    sLayer = (SlownessLayer)PLayers.elementAt(j);
	    if (Math.abs(sLayer.getTopP() - sLayer.getBotP()) > maxDeltaP) {
		numNewP = Math.ceil(Math.abs(sLayer.getTopP() - sLayer.getBotP()) 
				    / maxDeltaP);
		deltaP = (sLayer.getTopP() - sLayer.getBotP()) / numNewP;
				
		for (int rayNum=1; rayNum < numNewP; rayNum++) {
		    addSlowness(sLayer.getTopP()+rayNum*deltaP, PWAVE);
		    addSlowness(sLayer.getTopP()+rayNum*deltaP, SWAVE);
		}
	    }
	}
    }
   
    /** Checks to make sure no slowness layer spans more than 
     *  maxDepthInterval. */
    protected void depthIncCheck() 
	throws SlownessModelException, NoSuchLayerException 
    {
   	SlownessLayer sLayer;
   	int numNewDepths;
   	double deltaDepth;
   	double velocity;
   	double p;
   	
	try {
	    for (int j=0; j<SLayers.size(); j++) {
		sLayer = (SlownessLayer)SLayers.elementAt(j);
		if ((sLayer.getBotDepth() - sLayer.getTopDepth()) > maxDepthInterval) {
		    numNewDepths = (int)Math.ceil((sLayer.getBotDepth() - sLayer.getTopDepth()) / maxDepthInterval);
		    deltaDepth = (sLayer.getBotDepth() - sLayer.getTopDepth()) / numNewDepths;
		    for (int depthNum=1; depthNum < numNewDepths; depthNum++) {
			velocity = vMod.evaluateAbove(sLayer.getTopDepth() + depthNum*deltaDepth, 'S');
			if (velocity == 0.0 || (! allowInnerCoreS && 
						sLayer.getTopDepth()+ depthNum*deltaDepth >= vMod.getIocbDepth()) ) {
			    velocity = vMod.evaluateAbove(sLayer.getTopDepth() + depthNum*deltaDepth, 'P');
			}
			p = toSlowness(velocity, sLayer.getTopDepth() + depthNum*deltaDepth);
			addSlowness(p, PWAVE);
			addSlowness(p, SWAVE);
		    }
		}
	    }
	    for (int j=0; j<PLayers.size(); j++) {
		sLayer = (SlownessLayer)PLayers.elementAt(j);
		if ((sLayer.getBotDepth() - sLayer.getTopDepth()) > maxDepthInterval) {
		    numNewDepths = (int)Math.ceil((sLayer.getBotDepth() - sLayer.getTopDepth()) / maxDepthInterval);
		    deltaDepth = (sLayer.getBotDepth() - sLayer.getTopDepth()) / numNewDepths;
		    for (int depthNum=1; depthNum < numNewDepths; depthNum++) {
			p = toSlowness(vMod.evaluateAbove(sLayer.getTopDepth() + depthNum*deltaDepth, 'P'), 
				       sLayer.getTopDepth() + depthNum*deltaDepth);
			addSlowness(p, PWAVE);
			addSlowness(p, SWAVE);
		    }
		}
	    }
	} catch (NoSuchMatPropException e) {
	    // can't happen
	    e.printStackTrace();
	}
    }
   
    /** Checks to make sure no slowness layer spans more than maxRangeInterval
     *  and that the (estimated) error due to linear interpolation is less than
     *  maxInterpError. */
    protected void distanceCheck() throws SlownessModelException, NoSuchMatPropException, NoSuchLayerException {
   	SlownessLayer sLayer, prevSLayer;
   	int j;
	TimeDist prevTD;
	TimeDist currTD;
	TimeDist prevPrevTD;
	boolean isCurrOK;
	boolean isPrevOK;
	boolean currWaveType, otherWaveType;  // TRUE=P and FALSE=S

	/* do SWAVE and then PWAVE, waveN is ONLY used on the next 2 lines */
	for (int waveN=0; waveN < 2; waveN++) {
	    currWaveType = waveN==0 ? SWAVE : PWAVE;
	    otherWaveType = ! currWaveType;
	    prevPrevTD = null;
	    prevTD = null;
	    currTD = null;
	    isCurrOK = false;
	    isPrevOK = false;
	    j=0;
	    sLayer = getSlownessLayer(0, currWaveType);  // preset sLayer so prevSLayer is ok
	    while ( j < getNumLayers(currWaveType)) {
		prevSLayer = sLayer;
		sLayer = getSlownessLayer(j, currWaveType);
		if ( ! depthInHighSlowness(sLayer.getBotDepth(), sLayer.getBotP(), currWaveType) && 
		     ! depthInHighSlowness(sLayer.getTopDepth(), sLayer.getTopP(), currWaveType)) {
	
		    // Don't calculate prevTD if we can avoid it
		    if (isCurrOK ) {
			if (isPrevOK) {
			    prevPrevTD = prevTD;
			} else {
			    prevPrevTD = null;
			}
			prevTD = currTD;
			isPrevOK = true;
		    } else {
			prevTD = approxDistance(j-1, sLayer.getTopP(), currWaveType);
			isPrevOK = true;
		    }
		    currTD = approxDistance(j, sLayer.getBotP(), currWaveType);
		    isCurrOK = true;
	
		    // check for too great of distance jump
		    if ( Math.abs(prevTD.dist - currTD.dist) > maxRangeInterval &&
			 Math.abs(sLayer.getTopP() - sLayer.getBotP()) > 2*minDeltaP) {
			addSlowness((sLayer.getTopP()+sLayer.getBotP())/2.0, PWAVE);
			addSlowness((sLayer.getTopP()+sLayer.getBotP())/2.0, SWAVE);
			currTD = prevTD;
			prevTD = prevPrevTD;
		    } else {
			// make guess as to error estimate due to linear interpolation
			// if it is not ok, then we split both the previous and current
			// slowness layers, this has the nice, if unintended, consequense
			// of adding extra samples in the neighborhood of poorly sampled
			// caustics
			if (prevPrevTD != null && Math.abs(
							   prevTD.time - ((currTD.time-prevPrevTD.time)*
									  (prevTD.dist-prevPrevTD.dist)/
									  (currTD.dist - prevPrevTD.dist) + prevPrevTD.time)) > maxInterpError) {
	
			    addSlowness((prevSLayer.getTopP()+prevSLayer.getBotP())/2.0, PWAVE);
			    addSlowness((prevSLayer.getTopP()+prevSLayer.getBotP())/2.0, SWAVE);
			    addSlowness((sLayer.getTopP()+sLayer.getBotP())/2.0, PWAVE);
			    addSlowness((sLayer.getTopP()+sLayer.getBotP())/2.0, SWAVE);
			    currTD = prevPrevTD;
			    isPrevOK = false;
			    j--;
			    sLayer = getSlownessLayer( ((j-1>=0) ? j-1 : 0),currWaveType);
			    //     ^^^ make sure j != 0
			} else {
			    j++;
			    if (DEBUG && (j % 100 == 0)) { System.out.print(" "+j); }
			}
		    }
		} else {
		    prevPrevTD = null;
		    prevTD = null;
		    currTD = null;
		    isCurrOK = false;
		    isPrevOK = false;
		    j++;
		    if (DEBUG && (j % 100 == 0)) { System.out.print(" "+j); }
		}
	
	    }
	    if (DEBUG) {
		System.out.println("\nNumber of "+(currWaveType ? 'P' : 'S')+" slowness layers: "+j);
	    }
	}
    }
   
    /** Adds the given ray parameter, p, to the slowness sampling for the
     *  given waveType. It splits slowness layers as needed and keeps 
     *  P and S sampling consistant within fluid layers. Note, this makes 
     *  use of the velocity model, so all interpolation is linear in 
     *  velocity, not in slowness!
     *  @returns true if a change was made, false otherwise.
     */
    protected void addSlowness(double p, boolean isPWave) 
	throws SlownessModelException, NoSuchLayerException 
    {
   	boolean madeAChange = false;
   	Vector layers, otherLayers;
   	SlownessLayer sLayer, topLayer, botLayer;
   	double slope;
   	double topVelocity, botVelocity;
   	int otherIndex;
   	
   	if (isPWave) {
	    layers = PLayers;
	    otherLayers = SLayers;
   	} else {
	    layers = SLayers;
	    otherLayers = PLayers;
   	}
   	
   	for (int i=0; i< layers.size(); i++) {
	    sLayer = (SlownessLayer)layers.elementAt(i);
	    try {
		if (sLayer.getTopDepth() != sLayer.getBotDepth()) {
		    topVelocity = vMod.evaluateBelow(sLayer.getTopDepth(), 
						     (isPWave ? 'P' : 'S'));
		    botVelocity = vMod.evaluateAbove(sLayer.getBotDepth(), 
						     (isPWave ? 'P' : 'S'));
		} else {
		    // if depths are same we really only need topVelocity, 
		    // and just to verify that we are not in a fluid.
		    topVelocity = vMod.evaluateAbove(sLayer.getBotDepth(), 
						     (isPWave ? 'P' : 'S'));
		    botVelocity = vMod.evaluateBelow(sLayer.getTopDepth(), 
						     (isPWave ? 'P' : 'S'));
		}
	    } catch (NoSuchMatPropException e) {
		//Can't happen but...
		throw new SlownessModelException("Caught NoSuchMatPropException: "+
						 e.getMessage());
	    }


	    // We don't need to check for S waves in a fluid or 
	    // in inner core if allowInnerCoreS==false.
	    if (! isPWave) {
		if (! allowInnerCoreS && 
		    sLayer.getBotDepth() > vMod.getIocbDepth()) {
		    break;
		} else if (topVelocity == 0.0) {
		    continue;
		}
	    }

	    if ((sLayer.getTopP() - p)*(p - sLayer.getBotP()) > 0) {
		madeAChange = true;
		topLayer = (SlownessLayer)sLayer.clone();
		topLayer.setBotP(p);
		if (sLayer.getBotDepth() != sLayer.getTopDepth()) {
		    /* not a zero thickness layer, so calculate the depth for 
		     * the ray parameter. */
		    slope = (botVelocity - topVelocity ) / 
			(sLayer.getBotDepth()-sLayer.getTopDepth());
		    topLayer.setBotDepth(interpolate(p, 
						    topVelocity, 
						    sLayer.getTopDepth(), 
						    slope));

		}
		botLayer = (SlownessLayer)sLayer.clone();
		botLayer.setTopP(p);
		botLayer.setTopDepth(topLayer.getBotDepth());
		layers.removeElementAt(i);
		layers.insertElementAt(botLayer, i);
		layers.insertElementAt(topLayer, i);
		otherIndex = otherLayers.indexOf(sLayer);
		if (otherIndex != -1) {
		    otherLayers.removeElementAt(otherIndex);
		    otherLayers.insertElementAt(botLayer, otherIndex);
		    otherLayers.insertElementAt(topLayer, otherIndex);
		}
			
	    }
   	}
    }
   
    /** Resets the slowness layers that correspond to critical points.
     */
    protected void fixCriticalPoints() throws NoSuchLayerException {
   	CriticalDepth cd;
   	SlownessLayer sLayer;
   	for (int i=0; i< criticalDepthVector.size() ; i++) {
	    cd = (CriticalDepth)criticalDepthVector.elementAt(i);

	    cd.PLayerNum = layerNumberBelow(cd.depth, PWAVE);
	    sLayer = getSlownessLayer(cd.PLayerNum, PWAVE);
	    if (cd.PLayerNum == PLayers.size()-1 && sLayer.getBotDepth() == cd.depth) {
		cd.PLayerNum++;	// want the last critical point to be the bottom of the last layer
	    }
   		
	    cd.SLayerNum = layerNumberBelow(cd.depth, SWAVE);
	    sLayer = getSlownessLayer(cd.SLayerNum, SWAVE);
	    if (cd.SLayerNum == SLayers.size()-1 && sLayer.getBotDepth() == cd.depth) {
		cd.SLayerNum++;	// want the last critical point to be the bottom of the last layer
	    }
     	}
    }

    /** Finds the index of the slowness layer that contains the given depth
     *  Note that if the depth is a layer boundary, it returns the shallower
     *  of the two or possibly more (since total reflections are zero 
     *  thickness layers) layers.
     *
     *  @return the layer number.
     *  @exception NoSuchLayerException occurs if no layer in the slowness
     *     model contains the given depth.
     */
    public int layerNumberAbove(double depth, boolean isPWave)
	throws NoSuchLayerException {
	SlownessLayer tempLayer;
	Vector layers;
 
	if (isPWave) {
	    layers = PLayers;
	} else {
	    layers = SLayers;
	}

	// check surface first
	tempLayer = (SlownessLayer)layers.elementAt(0);
	if (tempLayer.getTopDepth() == depth) {
	    return 0;
	}
	if (depth < tempLayer.getTopDepth() || 
	    ((SlownessLayer)layers.elementAt(layers.size()-1)).getBotDepth() < depth) {
	    throw new NoSuchLayerException(depth);
	}

	int tooSmallNum = 0;
	int tooLargeNum = layers.size()-1;
	int currentNum = 0;
	boolean found = false;
	while ( ! found) {
	    currentNum = Math.round((tooSmallNum + tooLargeNum) / 2.0f);
	    tempLayer = getSlownessLayer(currentNum, isPWave);

	    if (tempLayer.getTopDepth() >= depth) {
		tooLargeNum = currentNum-1;
	    } else if (tempLayer.getBotDepth() < depth) {
		tooSmallNum = currentNum+1;
	    } else {
		found = true;
	    }
	}
	return currentNum;
    }
   
    /** Finds the index of the slowness layer that contains the given depth
     *  Note that if the depth is a layer boundary, it returns the deeper
     *  of the two or possibly more (since total reflections are zero 
     *  thickness layers) layers.
     *
     *  @return the layer number.
     *  @exception NoSuchLayerException occurs if no layer in the slowness
     *     model contains the given depth.
     */
    public int layerNumberBelow(double depth, boolean isPWave)
	throws NoSuchLayerException {
	SlownessLayer tempLayer;
	Vector layers;
 
	if (isPWave) {
	    layers = PLayers;
	} else {
	    layers = SLayers;
	}

	// check surface first
	tempLayer = (SlownessLayer)layers.elementAt(0);
	if (tempLayer.getTopDepth() == depth) {
	    return 0;
	}
      	// check bottommost layer
	tempLayer = (SlownessLayer)layers.elementAt(layers.size()-1);
	if (tempLayer.getBotDepth() == depth) {
	    return layers.size()-1;
	}
      	// check to make sure depth is within the range available
	if (depth < ((SlownessLayer)layers.elementAt(0)).getTopDepth() || 
	    ((SlownessLayer)layers.elementAt(layers.size()-1)).getBotDepth() < depth) {
	    throw new NoSuchLayerException(depth);
	}

	int tooSmallNum = 0;
	int tooLargeNum = layers.size()-1;
	int currentNum = 0;
	boolean found = false;
	while ( ! found) {
	    currentNum = Math.round((tooSmallNum + tooLargeNum) / 2.0f);
	    tempLayer = getSlownessLayer(currentNum, isPWave);

	    if (tempLayer.getTopDepth() > depth) {
		tooLargeNum = currentNum-1;
	    } else if (tempLayer.getBotDepth() <= depth) {
		tooSmallNum = currentNum+1;
	    } else {
		found = true;
	    }
	}
	return currentNum;
    }


    /** Performs consistency check on the slowness model.
     *  @return true if successful, throws SlownessModelException otherwise.
     *  @exception SlownessModelException if any check fails
     */
    public boolean validate() 
	throws SlownessModelException 
    {
	boolean isOK = true;
	double prevDepth;
	DepthRange highSZoneDepth, fluidZone;
	SlownessLayer sLayer;

	/* is radiusOfEarth positive? */
	if (radiusOfEarth <= 0.0) {
	    throw new SlownessModelException(
					     "Radius of earth is not positive. radiusOfEarth = "+radiusOfEarth);
	}

	/* is maxDepthInterval positive? */
	if (maxDepthInterval <= 0.0) {
	    throw new SlownessModelException(
					     "maxDepthInterval is not positive. maxDepthInterval = "+
					     maxDepthInterval);
	}
      
	/* Check for inconsistencies in high slowness zones. */
	Vector highSlownessLayerDepths = highSlownessLayerDepthsP;
	boolean isPWave = PWAVE;
	for (int j=0;j<2; j++, isPWave = SWAVE) {
	    if (isPWave) {
		highSlownessLayerDepths = highSlownessLayerDepthsP;
	    } else {
		highSlownessLayerDepths = highSlownessLayerDepthsS;
	    }

	    prevDepth= -1*Double.MAX_VALUE;
	    for (int i=0;i<highSlownessLayerDepths.size();i++) {
		highSZoneDepth = (DepthRange)highSlownessLayerDepths.elementAt(i);
		if (highSZoneDepth.topDepth >= highSZoneDepth.botDepth) {
		    throw new SlownessModelException(
						     "High slowness zone has zero or negative thickness. Num "+
						     i+" isPWave="+isPWave+
						     " top depth "+highSZoneDepth.topDepth+
						     " bottom depth "+highSZoneDepth.botDepth);
		}
		if (highSZoneDepth.topDepth <= prevDepth) {
		    throw new SlownessModelException
			("High slowness zone overlaps previous zone. Num "+
			 i+" isPWave="+isPWave+
			 " top depth "+highSZoneDepth.topDepth+
			 " bottom depth "+highSZoneDepth.botDepth);
		}
		prevDepth = highSZoneDepth.botDepth;
	    }
	}

	/* Check for inconsistencies in fluid zones. */
	prevDepth= -1*Double.MAX_VALUE;
	for (int i=0;i<fluidLayerDepths.size();i++) {
	    fluidZone = (DepthRange)fluidLayerDepths.elementAt(i);
	    if (fluidZone.topDepth >= fluidZone.botDepth) {
		throw new SlownessModelException(
						 "Fluid zone has zero or negative thickness. Num "+
						 i+" top depth "+fluidZone.topDepth+
						 " bottom depth "+fluidZone.botDepth);
	    }
	    if (fluidZone.topDepth <= prevDepth) {
		throw new SlownessModelException
		    ("Fluid zone overlaps previous zone. Num "+
		     i+" top depth "+fluidZone.topDepth+
		     " bottom depth "+fluidZone.botDepth);
	    }
	    prevDepth = fluidZone.botDepth;
	}

	/* Check for inconsistencies in slowness layers. */
	isPWave = PWAVE;
	double prevBotP;
	for (int j=0;j<2; j++, isPWave = SWAVE) {

	    prevDepth= 0.0;
	    if (getNumLayers(isPWave) > 0) {
		prevBotP = getSlownessLayer(0, isPWave).getTopP();
	    } else {
		prevBotP = -1;
	    }
	    for (int i=0;i<getNumLayers(isPWave);i++) {
		sLayer = getSlownessLayer(i, isPWave);
		isOK &= sLayer.validate();

		if (sLayer.getTopDepth() > prevDepth) {
		    throw new SlownessModelException(
						     "Gap of "+(sLayer.getTopDepth()-prevDepth)+
						     " between slowness layers. Num "+
						     i+" isPWave="+isPWave+
						     " top depth "+sLayer.getTopDepth()+
						     " bottom depth "+sLayer.getBotDepth());
		}
		if (sLayer.getTopDepth() < prevDepth) {
		    throw new SlownessModelException(
						     "Slowness layer overlaps previous layer by "+
						     (prevDepth-sLayer.getTopDepth())+". Num "+
						     i+" isPWave="+isPWave+
						     " top depth "+sLayer.getTopDepth()+
						     " bottom depth "+sLayer.getBotDepth());
		}
		if (sLayer.getTopP() != prevBotP) {
		    throw new SlownessModelException(
						     "Slowness layer gap/overlaps previous layer in slowness "+
						     ". Num "+ i+" isPWave="+isPWave+
						     " prevBotP= "+prevBotP+
						     " sLayer= "+sLayer);
		}
		if (Double.isNaN(sLayer.getTopDepth())) {
		    throw new SlownessModelException(
						     "Top depth is NaN, layerNum="+i+
						     " waveType="+(isPWave?'P':'S'));
		}
		if (Double.isNaN(sLayer.getBotDepth())) {
		    throw new SlownessModelException(
						     "Top depth is NaN, layerNum="+i+
						     " waveType="+(isPWave?'P':'S'));
		}
		prevBotP = sLayer.getBotP();
		prevDepth = sLayer.getBotDepth();
	    }
	}
		
	/* Everything checks out OK so return true. */
	return isOK;
    }

    /*
   
      public void writeToStream(String filename) throws IOException {
      DataOutputStream dos = new DataOutputStream(
      new BufferedOutputStream( new FileOutputStream(filename)));
      writeToStream(dos);
      dos.close();
      }
   
      public void writeToStream(DataOutputStream dos) throws IOException{
      DepthRange dr;
      SlownessLayer sl;
      
      dos.writeInt(getClass().getName().length());
      dos.writeBytes(getClass().getName());

      dos.writeDouble(radiusOfEarth);
      vMod.writeToStream(dos);
      dos.writeInt(criticalVelLayers.size());
      for (int i=0;i<criticalVelLayers.size();i++) {
      dos.writeInt(((Integer)criticalVelLayers.elementAt(i)).intValue());
      }
      dos.writeInt(criticalLayers.size());
      for (int i=0;i<criticalLayers.size();i++) {
      dos.writeInt(((Integer)criticalLayers.elementAt(i)).intValue());
      }
      dos.writeInt(highSlownessLayerDepths.size());
      for (int i=0;i<highSlownessLayerDepths.size();i++) {
      dr = (DepthRange)highSlownessLayerDepths.elementAt(i);
      dos.writeDouble(dr.topDepth);
      dos.writeDouble(dr.botDepth);
      dos.writeDouble(dr.rayParam);
      }
      dos.writeInt(fluidLayerDepths.size());
      for (int i=0;i<fluidLayerDepths.size();i++) {
      dr = (DepthRange)fluidLayerDepths.elementAt(i);
      dos.writeDouble(dr.topDepth);
      dos.writeDouble(dr.botDepth);
      dos.writeDouble(dr.rayParam);
      }
      dos.writeInt(slowness.size());
      for (int i=0;i<slowness.size();i++) {
      sl = (SlownessLayer)slowness.elementAt(i);
      dos.writeDouble(sl.topDepth);
      dos.writeDouble(sl.botDepth);
      dos.writeDouble(sl.topP);
      dos.writeDouble(sl.botP);
      }
      dos.writeDouble(maxDeltaP);
      dos.writeDouble(minDeltaP);
      dos.writeDouble(maxDepthInterval);
      dos.writeDouble(maxRangeInterval);
      dos.writeDouble(minRangeInterval);
      dos.writeDouble(minRadiusCheck);
      dos.writeBoolean(allowInnerCoreS);
      dos.writeDouble(minInnerCoreDepth);
      dos.writeDouble(slownessTolerance);
      }
      
      public static SlownessModel readFromStream(String filename) 
      throws FileNotFoundException, IOException, ClassNotFoundException,
      IllegalAccessException, InstantiationException {
      DataInputStream dis = new DataInputStream(
      new BufferedInputStream( new FileInputStream(filename)));
      SlownessModel sMod = readFromStream(dis);
      dis.close();
      return sMod;
      }
   
      public static SlownessModel readFromStream(DataInputStream dis) 
      throws IOException, InstantiationException, IllegalAccessException,
      ClassNotFoundException {
      int length;
      
 
      byte[] classString = new byte[dis.readInt()];
      dis.read(classString);
      Class sModClass = Class.forName(new String(classString));
      SlownessModel sMod = (SlownessModel)sModClass.newInstance();
 
      sMod.vMod = VelocityModel.readFromStream(dis);
      length = dis.readInt();
      sMod.criticalVelLayers = new Vector(length);
      for (int i=0;i<length;i++) {
      sMod.criticalVelLayers.addElement(new Integer(dis.readInt()));
      }
      length = dis.readInt();
      sMod.criticalLayers = new Vector(length);
      for (int i=0;i<length;i++) {
      sMod.criticalLayers.addElement(new Integer(dis.readInt()));
      }
      length = dis.readInt();
      sMod.highSlownessLayerDepths = new Vector(length);
      DepthRange dr;
      for (int i=0;i<length;i++) {
      dr = new DepthRange();
      dr.topDepth = dis.readDouble();
      dr.botDepth = dis.readDouble();
      dr.rayParam = dis.readDouble();
      sMod.highSlownessLayerDepths.addElement(dr);
      }
      length = dis.readInt();
      sMod.fluidLayerDepths = new Vector(length);
      for (int i=0;i<length;i++) {
      dr = new DepthRange();
      dr.topDepth = dis.readDouble();
      dr.botDepth = dis.readDouble();
      dr.rayParam = dis.readDouble();
      sMod.fluidLayerDepths.addElement(dr);
      }
      
      length = dis.readInt();
      sMod.slowness = new Vector(length);
      SlownessLayer sl;
      for (int i=0;i<length;i++) {
      sl = new SlownessLayer();
      sl.topDepth = dis.readDouble();
      sl.botDepth = dis.readDouble();
      sl.topP = dis.readDouble();
      sl.botP = dis.readDouble();
      sMod.slowness.addElement(sl);
      }
      
      sMod.maxDeltaP = dis.readDouble();
      sMod.minDeltaP = dis.readDouble();
      sMod.maxDepthInterval = dis.readDouble();
      sMod.maxRangeInterval = dis.readDouble();
      sMod.minRangeInterval = dis.readDouble();
      sMod.minRadiusCheck = dis.readDouble();
      sMod.allowInnerCoreS = dis.readBoolean();
      sMod.minInnerCoreDepth = dis.readDouble();
      sMod.slownessTolerance = dis.readDouble();
      return sMod;
      }
    */
    
    /* Returns a clone of this slowness model. All fields are correctly
     * copied so modifications to the clone do not affect the original. */
    public Object clone() {
	SlownessModel newObject;
	try {
	    newObject = (SlownessModel)super.clone();

	    newObject.criticalDepthVector = new Vector(criticalDepthVector.size());
	    for (int i=0;i<criticalDepthVector.size();i++) {
		newObject.criticalDepthVector.addElement(
							 ((CriticalDepth)criticalDepthVector.elementAt(i)).clone());
	    }

	    newObject.highSlownessLayerDepthsP = 
		new Vector(highSlownessLayerDepthsP.size());
	    for (int i=0;i<highSlownessLayerDepthsP.size();i++) {
		newObject.highSlownessLayerDepthsP.addElement(
							      ((DepthRange)highSlownessLayerDepthsP.elementAt(i)).clone());
	    }

	    newObject.highSlownessLayerDepthsS = 
		new Vector(highSlownessLayerDepthsS.size());
	    for (int i=0;i<highSlownessLayerDepthsS.size();i++) {
		newObject.highSlownessLayerDepthsS.addElement(
							      ((DepthRange)highSlownessLayerDepthsS.elementAt(i)).clone());
	    }

	    newObject.fluidLayerDepths = new Vector(fluidLayerDepths.size());
	    for (int i=0;i<fluidLayerDepths.size();i++) {
		newObject.fluidLayerDepths.addElement(
						      ((DepthRange)fluidLayerDepths.elementAt(i)).clone());
	    }

	    newObject.PLayers = new Vector(getNumLayers(PWAVE));
	    for (int i=0;i<getNumLayers(PWAVE);i++) {
		newObject.PLayers.addElement( getSlownessLayerClone(i, PWAVE));
	    }

	    newObject.SLayers = new Vector(getNumLayers(SWAVE));
	    for (int i=0;i<getNumLayers(SWAVE);i++) {
		newObject.SLayers.addElement( getSlownessLayerClone(i, SWAVE));
	    }
	    return newObject;

	} catch (CloneNotSupportedException e) {
	    // Can't happen, but...
	    System.err.println("Caught CloneNotSupportedException: "+
			       e.getMessage());
	    throw new InternalError(e.toString());
	}
      
    }

    public String toString() {
	int topCriticalLayerNum;
	int botCriticalLayerNum;
	String desc = "";
		
	desc = "radiusOfEarth="+radiusOfEarth+
	    "\n maxDeltaP="+maxDeltaP+
	    "\n minDeltaP="+minDeltaP+
	    "\n maxDepthInterval="+maxDepthInterval+
	    "\n maxRangeInterval="+maxRangeInterval+
	    "\n allowInnerCoreS="+allowInnerCoreS+
	    "\n slownessTolerance="+slownessTolerance+
	    "\n getNumLayers('P')="+getNumLayers(PWAVE)+
	    "\n getNumLayers('S')="+getNumLayers(SWAVE)+
	    "\n fluidLayerDepths.size()="+fluidLayerDepths.size()+
	    "\n highSlownessLayerDepthsP.size()="+
	    highSlownessLayerDepthsP.size()+
	    "\n highSlownessLayerDepthsS.size()="+
	    highSlownessLayerDepthsS.size()+
	    "\n criticalDepthVector.size()="+criticalDepthVector.size()+
	    "\n";

	if (criticalDepthVector.size()!=0) {
	    desc += ("**** Critical Depth Layers ************************\n");
	    botCriticalLayerNum = 
		((CriticalDepth)criticalDepthVector.elementAt(0)).velLayerNum-1;
	    for (int criticalNum=1;criticalNum<criticalDepthVector.size();
		 criticalNum++) {
		topCriticalLayerNum = botCriticalLayerNum+1;
		botCriticalLayerNum = 
		    ((CriticalDepth)criticalDepthVector.elementAt(criticalNum)).velLayerNum-1;
		desc += " "+topCriticalLayerNum+","+botCriticalLayerNum;
	    }
	}
	desc += "\n";

	if (fluidLayerDepths.size()!=0) {
	    desc+="\n**** Fluid Layer Depths ************************\n";
	    for (int i=0;i<fluidLayerDepths.size();i++) {
		desc+=((DepthRange)fluidLayerDepths.elementAt(i)).topDepth+","+
		    ((DepthRange)fluidLayerDepths.elementAt(i)).botDepth+" ";
	    }
	}
	desc += "\n";

	if (highSlownessLayerDepthsP.size()!=0) {
	    desc+="\n**** P High Slowness Layer Depths ****************\n";
	    for (int i=0;i<highSlownessLayerDepthsP.size();i++) {
		desc+=((DepthRange)highSlownessLayerDepthsP.elementAt(i)).topDepth+","+
		    ((DepthRange)highSlownessLayerDepthsP.elementAt(i)).botDepth+" ";
	    }
	}
	desc += "\n";

	if (highSlownessLayerDepthsS.size()!=0) {
	    desc+="\n**** S High Slowness Layer Depths ****************\n";
	    for (int i=0;i<highSlownessLayerDepthsS.size();i++) {
		desc+=((DepthRange)highSlownessLayerDepthsS.elementAt(i)).topDepth+","+
		    ((DepthRange)highSlownessLayerDepthsS.elementAt(i)).botDepth+" ";
	    }
	}
	desc += "\n";

	return desc;
    }
}

