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

/**
 * SplitLayerInfo.java
 *
 *
 * Created: Wed Feb  3 11:32:14 1999
 *
 * @author Philip Crotwell
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



 */
package edu.sc.seis.TauP;

public class SplitLayerInfo implements java.io.Serializable, Cloneable {
    
    boolean neededSplit;

    boolean movedSample;

    double rayParam;

    public SplitLayerInfo(boolean neededSplit,
			  boolean movedSample,
			  double rayParam) {
	this.neededSplit = neededSplit;
	this.movedSample = movedSample;
	this.rayParam = rayParam;
    }
    

    /**
       * Get the value of neededSplit.
       * @return Value of neededSplit.
       */
    public boolean getNeededSplit() {return neededSplit;}
    
    /**
       * Set the value of neededSplit.
       * @param v  Value to assign to neededSplit.
       */
    public void setNeededSplit(boolean  v) {this.neededSplit = v;}
    

    /**
       * Get the value of movedSample.
       * @return Value of movedSample.
       */
    public boolean getMovedSample() {return movedSample;}
    
    /**
       * Set the value of movedSample.
       * @param v  Value to assign to movedSample.
       */
    public void setMovedSample(boolean  v) {this.movedSample = v;}
    
   
    /**
       * Get the value of rayParam.
       * @return Value of rayParam.
       */
    public double getRayParam() {return rayParam;}
    
    /**
       * Set the value of rayParam.
       * @param v  Value to assign to rayParam.
       */
    public void setRayParam(double  v) {this.rayParam = v;}
    
} // SplitLayerInfo
