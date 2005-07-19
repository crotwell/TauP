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
 * TauP_WKBJ.java
 *
 *
 *
 *
 * @author Philip Crotwell
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



 */

package edu.sc.seis.TauP;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import edu.sc.seis.seisFile.sac.SacTimeSeries;

public class TauP_WKBJ extends TauP_Time {
    
    /** deltaT of the seismogram, default is .05 which gives
    20 sps. */
    protected double deltaT = .05;

    /** number of samples in the seismogram. Default is 100. */
    protected int numSamples = 1000;

    /** start time of the seismogram relative to the origin time.
    default is 0. */
    protected double startTime = 0;

    public TauP_WKBJ() {
    super();
    }
    
    public TauP_WKBJ(TauModel tMod) throws TauModelException {
    super(tMod);
    }
    
    public TauP_WKBJ(String modelName) throws TauModelException {
    super(modelName);
    }
    
    /**
       * Get the value of deltaT.
       * @return Value of deltaT.
       */
    public double getDeltaT() {return deltaT;}
    
    /**
       * Set the value of deltaT.
       * @param v  Value to assign to deltaT.
       */
    public void setDeltaT(double  v) {this.deltaT = v;}
    
    /**
       * Get the value of numSamples.
       * @return Value of numSamples.
       */
    public int getNumSamples() {return numSamples;}
    
    /**
       * Set the value of numSamples.
       * @param v  Value to assign to numSamples.
       */
    public void setNumSamples(int  v) {this.numSamples = v;}
 
    /**
       * Get the value of startTime.
       * @return Value of startTime.
       */
    public double getStartTime() {return startTime;}
    
    /**
       * Set the value of startTime.
       * @param v  Value to assign to startTime.
       */
    public void setStartTime(double  v) {this.startTime = v;}
    

    public void calculate(double degrees) throws TauModelException {
    recalcPhases();
    clearArrivals();
    calcWKBJ(degrees);
    }

    public void calcWKBJ(double degrees) throws TauModelException {
    this.degrees = degrees;
 System.out.println("In calcWKBJ for "+degrees+" degrees.");
 
    SeismicPhase phase;
    Arrival[] phaseArrivals;
    double calcTime, calcDist;
    Theta thetaAtX;
    double rayParam, nextRayParam, minRayParam;
    double theta, nextTheta;
//  WKBJArrival seismogramArrival;
    ReflTransCoefficient rtCoeff;

    for (int phaseNum=0;phaseNum<phases.size(); phaseNum++) {
System.out.println("Phase "+((SeismicPhase)phases.elementAt(phaseNum)).getName()+".");
        phase = (SeismicPhase)phases.elementAt(phaseNum);
        phase.setDEBUG(DEBUG);
        phase.calcTime(degrees);
        minRayParam = phase.getMinRayParam();
//      rtCoeff = new ReflTransCoefficient(phase);
        if (phase.hasArrivals()) {
    

        phaseArrivals = phase.getArrivals();

        for (int i=0;i<phaseArrivals.length;i++) {
System.out.println("Arrival number "+i);
            thetaAtX = new Theta(phase, phaseArrivals[i].getDist());
System.out.println("Got Theta");
            float[] seismogramPoints = new float[numSamples];
            rayParam = thetaAtX.getMaxRayParam();
System.out.println("Got ray param");
            theta = thetaAtX.getTheta(rayParam);
System.out.println("Got theta for ray param");
            setStartTime(320);
            nextRayParam = thetaAtX.getStepRayParam(rayParam,
                                getDeltaT());
            nextTheta = thetaAtX.getTheta(nextRayParam);
            int n = 0;
            try {
            while (nextRayParam >= minRayParam) {
                //System.out.println(n+"  "+rayParam+"  "+theta+"  "+nextRayParam+"  "+nextTheta);
                n = (int)Math.round((theta-getStartTime()) /
                        getDeltaT());
                if (n >= 0 && n < seismogramPoints.length) {
//              seismogramPoints[n] += (float)(
//                  Math.sqrt(rayParam)*
//                  rtCoeff.getCoefficient(rayParam)*
//                  (rayParam- nextRayParam));
System.out.println(n+"  "+seismogramPoints[n]);
 
                }
                rayParam = nextRayParam;
                theta = nextTheta;
                nextRayParam = thetaAtX.getStepRayParam(rayParam,
                                    getDeltaT());
                nextTheta = thetaAtX.getTheta(nextRayParam);
            }
            } catch (ArrayIndexOutOfBoundsException e) {
            // must have dropped off of end of theta curve
System.out.println("ArrayIndexOutOfBoundsException: "+e);
            }
//          seismogramArrival = new WKBJArrival( phaseArrivals[i],
//                           seismogramPoints);
            SacTimeSeries sac = new SacTimeSeries();
            sac.y = seismogramPoints;
            sac.npts = seismogramPoints.length;
            sac.leven = SacTimeSeries.TRUE;
            sac.iftype = SacTimeSeries.ITIME;
            sac.delta = (float)getDeltaT();
            sac.t0 = (float)phaseArrivals[i].getTime();
            sac.kt0 = phaseArrivals[i].getName();
            sac.o = 0;
            sac.b = 320;
            sac.e = sac.b + (sac.npts-1)* sac.delta;

            try {
            sac.write("tempsacfile");
            } catch (IOException e) {
            }
//          arrivals.addElement(seismogramArrival);
        }
        }
    }
    }

     /** Allows TauP_Time to run as an application. Creates an instance
       * of TauP_Time.
       * . */
   public static void main(String[] args)
      throws FileNotFoundException,
             IOException,
             StreamCorruptedException,
             ClassNotFoundException,
             OptionalDataException
   {
      try {
         long prevTime = 0;
         long currTime;

         prevTime = System.currentTimeMillis();
         TauP_Time tauPTime = new TauP_WKBJ();
         String[] noComprendoArgs = tauPTime.parseCmdLineArgs(args);
         if (noComprendoArgs.length > 0) {
            for (int i=0;i<noComprendoArgs.length;i++) {
               if (noComprendoArgs[i].equals("-help") ||
                    noComprendoArgs[i].equals("-version")) {
                  System.exit(0);
               }
            }
            System.out.println("I don't understand the following arguments, continuing:");
            for (int i=0;i<noComprendoArgs.length;i++) {
               System.out.print(noComprendoArgs[i]+" ");
            }
            System.out.println();
            noComprendoArgs = null;
         }
         currTime = System.currentTimeMillis();
 
         prevTime = System.currentTimeMillis();
         tauPTime.init();
         currTime = System.currentTimeMillis();
         if (tauPTime.DEBUG) {
            System.out.println("taup model read time="+(currTime-prevTime));
         }

         tauPTime.start();
         tauPTime.destroy();

      } catch (TauModelException e) {
         System.out.println("Caught: "+e.getMessage());
            e.printStackTrace();
      } catch (TauPException e) {
         System.out.println("Caught: "+e.getMessage());
            e.printStackTrace();
      }

   }

} // TauP_WKBJ
