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

/**
  * Class to hold a single slowness layer sample.
  *
  * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



  * @author H. Philip Crotwell
 */
public class SlownessLayer implements Serializable, Cloneable {

      /** Slowness at the top of the layer. */
   public double topP;

      /** Slowness at the bottom of the layer. */
   public double botP;

      /** Depth at the top of the layer. */
   public double topDepth;

      /** Depth at the bottom of the layer. */
   public double botDepth;

    /** top slowness, top depth, bottom slowness, bottom depth */
    public SlownessLayer(double topP, double topDepth, 
                         double botP, double botDepth) {
	Assert.isFalse(topDepth > botDepth, 
      "topDepth > botDepth: "+topDepth+" > "+botDepth);
	Assert.isFalse(topDepth < 0.0 
                  || Double.isNaN(topDepth) 
                  || Double.isInfinite(topDepth),
	               "topDepth is not a number or is negative: "+topDepth);
	Assert.isFalse(botDepth < 0.0 
                  || Double.isNaN(botDepth) 
                  || Double.isInfinite(botDepth),
	               "botDepth is not a number or is negative: "+botDepth);
	this.topP = topP;
	this.topDepth=topDepth;
	this.botP = botP;
	this.botDepth=botDepth;
    }

    /** Compute the slowness layer from a velocity layer.
       *       */
    public SlownessLayer(VelocityLayer vLayer, boolean spherical,
			 double radiusOfEarth, boolean isPWave) {
	Assert.isFalse(vLayer.topDepth > vLayer.botDepth,
		       "vLayer.topDepth > vLayer.botDepth :"+
		       vLayer.topDepth+ " "+vLayer.botDepth);
	topDepth = vLayer.topDepth;
	botDepth = vLayer.botDepth;

	char waveType;
	if (isPWave) { waveType = 'P';
	} else { waveType = 'S'; }
		
	try {
	    if (spherical) {
		topP = (radiusOfEarth-topDepth) /
		    vLayer.evaluateAtTop(waveType);
		botP = (radiusOfEarth-botDepth) /
		    vLayer.evaluateAtBottom(waveType);

	    } else {
		topP = 1.0/vLayer.evaluateAtTop(waveType);
		botP = 1.0/vLayer.evaluateAtBottom(waveType);
	    }
	    Assert.isFalse(Double.isNaN(topP) || Double.isNaN(botP),
			   "Slowness sample is NaN: topP="+topP+" botP="+botP);
	} catch (NoSuchMatPropException e) {
	    // Can't happen
	    e.printStackTrace();
	}
    }

      /** Compute the slowness layer from a velocity layer.
       *  Since radiusOfEarth is given we assume a spherical model.
       */
   public SlownessLayer(VelocityLayer vLayer, boolean isPWave,
         double radiusOfEarth) {
      this(vLayer, true, radiusOfEarth, isPWave);
   }

      /** Compute the slowness layer from a velocity layer. Since 
       *  radiusOfEarth is not given we assume a flat model.
       */
   public SlownessLayer(VelocityLayer vLayer, boolean isPWave) {
      this(vLayer, false, 0.0, isPWave);
   }

      /** Is the layer a zero thickness layer, ie a total reflection? */
   public boolean isZeroThickness() {
      if (topDepth == botDepth) {
         return true;
      } else {
         return false;
      }
   }

      /** Finds the slowness at the given depth. radiusOfEarth is needed
       *  as a slowness layer doesn't have access to the slowness model.
       *  Note that this method assumes
       *  a Bullen type of slowness interpolation, ie p(r) = a*r^b. This
       *  will produce results consistent with a tau model that uses this
       *  interpolant, but it may differ slightly from going directly to
       *  the velocity model. Also, if the tau model is generated using
       *  another interpolant, linear for instance, then the result may
       *  not be consistent with the tau model. */
   public double evaluateAt_bullen(double depth, double radiusOfEarth) 
         throws SlownessModelException
   {
      Assert.isFalse(botDepth>radiusOfEarth,
            "SlownessLayer.evaluateAt_bullen:"+
            " radiusOfEarth="+radiusOfEarth+
            " is smaller than the maximum depth of this layer."+
            " topDepth="+topDepth+" botDepth="+botDepth);

      Assert.isFalse((topDepth-depth)*(depth-botDepth)<0.0,
            "SlownessLayer.evaluateAt_bullen:"+
            " depth="+depth+" is not contained within this layer."+
            " topDepth="+topDepth+" botDepth="+botDepth);

      if (depth == topDepth) {
	  return topP;
      } else if (depth == botDepth) {
	  return botP;
      } else {
	  double B = Math.log(topP/botP) /
	      Math.log((radiusOfEarth-topDepth) / 
		       (radiusOfEarth-botDepth));
	  double A = topP/Math.pow((radiusOfEarth-topDepth), B);
	  double answer = A * Math.pow((radiusOfEarth-depth), B);
	  if (answer < 0.0 
	      || Double.isNaN(answer) 
	      || Double.isInfinite(answer)) {
	      // numerical instability in power law calculation???
	      // try a linear interpolation if the layer is small ( <2 km).
	      if ((botDepth- topDepth) < 2.0) {
		  double linear = 
		      (botP-topP)/(botDepth-topDepth)*(depth-topDepth) 
		      + topP;
		  if (linear < 0.0 
		      || Double.isNaN(linear) 
		      || Double.isInfinite(linear)) {
		  } else {
		      return linear;
		  }
	      }
	      throw new SlownessModelException(
"calculated slowness is not a number or is negative: "+
answer+"\n"+this.toString()+"\n A="+A+"   B="+B);
	  }
	  return answer;
      }
   }

      /** Calculates the time and distance (in radians) increments accumulated
       *  by a ray of spherical ray parameter p when passing through this layer.
       *  Note that this gives 1/2 of the true range and time increments since
       *  there will be both an up going and a downgoing path.
       *  Here we use the Mohorovicic or Bullen law p=A*r^B
       *
       *  @exception SlownessModelException occurs if the calculated
       *     distance or time increments are negative or NaN, this indicates a
       *     bug in the code (and hopefully will never happen).
       */
   public TimeDist bullenRadialSlowness(double p, double radiusOfEarth)
      throws SlownessModelException
   {
         // To hold the return values.
      TimeDist timedist = new TimeDist(p);
 
      if (topDepth == botDepth) {
	  timedist.dist = 0.0;
	  timedist.time = 0.0;
	  return timedist;
      }
      
      // only do bullen radial slowness if the layer is not too thin
      // here we use 1 micron = .000000001
      // just return 0 in this case
      if (botDepth - topDepth < .000000001) {
	  return timedist;
      }
	  
      double B = Math.log(topP/botP) /
         Math.log((radiusOfEarth-topDepth) /
                  (radiusOfEarth-botDepth));
      double sqrtTopTopMpp = Math.sqrt(topP*topP-p*p);
      double sqrtBotBotMpp = Math.sqrt(botP*botP-p*p);
 
      timedist.dist = 1/B*( 
			   Math.atan2(p, sqrtBotBotMpp)-
			   Math.atan2(p, sqrtTopTopMpp));

      timedist.time = 1/B*(sqrtTopTopMpp - sqrtBotBotMpp);

      if (timedist.dist<0.0 || timedist.time<0.0 ||
             Double.isNaN(timedist.time) || Double.isNaN(timedist.dist)) {
         throw new SlownessModelException("timedist <0.0 or NaN: "+
            "\n RayParam= "+p+
            "\n topDepth = "+topDepth+
            "\n botDepth = "+botDepth+
            "\n dist="+timedist.dist+
            "\n time="+timedist.time+
            "\n topP = "+topP+
            "\n botP = "+botP+
            "\n B = "+B+" "+toString());
      }
      return timedist;
   }

    /** Finds the depth for a ray parameter within this layer. Uses
     *  a Bullen interpolant, Ar^B. 
     *  Special case for botP == 0 or botDepth == radiusOfEarth as
     *  these cause div by 0, use linear interpolation in this
     *  case. */
   public double bullenDepthFor(double rayParam, double radiusOfEarth) 
   throws SlownessModelException {
      if ((topP-rayParam)*(rayParam-botP) >= 0) {
	  double tempDepth;

	  // easy case for 0 thickness layer
	  if (topDepth == botDepth) {
	      return botDepth;
	  }

	  if (botP != 0.0 && botDepth != radiusOfEarth) {
	      double B = Math.log(topP/botP) /
		  Math.log((radiusOfEarth-topDepth)/(radiusOfEarth-botDepth));
	      double A = topP/Math.pow((radiusOfEarth-topDepth), B);
	      tempDepth = radiusOfEarth - Math.exp(1.0/B * Math.log(rayParam/A) );
/*
	      tempDepth = radiusOfEarth - Math.pow(rayParam/A, 1.0/B);
*/
	      if (tempDepth < 0.0 
		  || Double.isNaN(tempDepth) 
		  || Double.isInfinite(tempDepth)
		  || tempDepth < topDepth
		  || tempDepth > botDepth) {
		  // numerical instability in power law calculation???
		  // try a linear interpolation if the layer is small ( <5 km).
		  if ((botDepth- topDepth) < 5.0) {
		      double linear = 
			  (botDepth-topDepth)/(botP-topP)*(rayParam-topP) 
			  + topDepth;
		      if (linear < 0.0 
			  || Double.isNaN(linear) 
			  || Double.isInfinite(linear)) {
		      } else {
			  return linear;
		      }
		  }
		  throw new SlownessModelException("claculated depth is not a number or is negative: "+tempDepth+"\n"+this+"\n"+A+"  "+B+"\n"+rayParam);
	      }
	      // check for tempDepth just above top depth
	      if (tempDepth < topDepth && (topDepth - tempDepth) < 1e-10) {
		  return topDepth;
	      }
	      // check for tempDepth just below bottom depth
	      if (tempDepth > botDepth && (tempDepth- botDepth) < 1e-10) {
		  return botDepth;
	      }
	      return tempDepth;
	  } else {
	      // a special case for the center of the earth, since ar^b 
	      // might blow up at r=0
	      if (topP != botP) {
		  return botDepth + (rayParam - botP) * (topDepth - botDepth) /
		      (topP - botP);
	      } else {
		  // weird case, return botDepth???
		  return botDepth;
	      }
	  }
      } else {
	  throw new SlownessModelException("Ray parameter = "+rayParam+
                  " is not contained within this slowness layer. topP="+
		  topP+" botP="+botP);
      }
   }
 
   public Object clone() {
      SlownessLayer newObject;
      try {
         newObject = (SlownessLayer)super.clone();
         return newObject;
      } catch (CloneNotSupportedException e) {
         // Can't happen, but...
         System.err.println("Caught CloneNotSupportedException: "+
            e.getMessage());
         throw new InternalError(e.toString());
      }
   }

      /** returns a String description of this SlownessLayer. */
   public String toString() {
//      String desc = "top p "+ (float)topP +", topDepth " + (float)topDepth
//                   +", bot p "+ (float)botP +", botDepth " + (float)botDepth;
      String desc = "top p "+ topP +", topDepth " + topDepth
                   +", bot p "+ botP +", botDepth " + botDepth;
      return desc;
   }

	public boolean validate() throws SlownessModelException {
      if (Double.isNaN(topP) || Double.isNaN(topDepth) ||
		Double.isNaN(botP) || Double.isNaN(botDepth)) {
         throw new SlownessModelException(
            "Slowness layer has NaN values."+"\n "+this);
		}
      if (topP < 0.0 || botP < 0.0) {
         throw new SlownessModelException(
            "Slowness layer has negative slownesses. \n "+this);
      }
      if (topDepth > botDepth) {
         throw new SlownessModelException
            ("Slowness layer has negative thickness. \n"+this);
      }
		return true;
	}
}
