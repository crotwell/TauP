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
 * WKBJArrival.java
 *
 *
 * Created: Mon Feb 15 13:06:36 1999
 *
 * @author Philip Crotwell
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



 */

package edu.sc.seis.TauP;

public class WKBJArrival extends Arrival {
    
    public WKBJArrival() {
	
    }
        
    public WKBJArrival(Arrival arrival, float[] seismogramPoints) {
	this.seismogramPoints = seismogramPoints;

	// these need to be updated if Arrival changes
	time = arrival.time;
	dist = arrival.dist;
	rayParam = arrival.rayParam;
	rayParamIndex = arrival.rayParamIndex;
	name = arrival.name;
	puristName = arrival.puristName;
	sourceDepth = arrival.sourceDepth;
	pierce = arrival.pierce;
	path = arrival.path;
    }

    public float[] getSeismogramPoints() {
	return seismogramPoints;
    }

    // protected below here -----------------------------------

    protected float[] seismogramPoints;

} // WKBJArrival
