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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.Vector;
import edu.sc.seis.seisFile.sac.SacTimeSeries;

/**
  *  Calculate times for phases and set sac headers based on gcarc or dist or
  *  station lat and lon and event lat and lon.
  *
  *  Note that triplicated phases will cause problems, as there is only one
  *  spot to put a time. An improved method would allow a phase to have several
  *  t#'s associated with it, so that all arrivals could be marked. Currently
  *  however, only the first arrival for a phase name is used.
  *
  *  Warning: I assume the evdp header has depth in meters unless the -evdpkm
  *  flag is set, in which case I assume kilometers. This may be a problem for
  *  users that improperly use kilometers for the depth units. Due to much
  *  abuse of the sac depth header units I output a warning message if the depth
  *  appears to be in kilometers, ie it is < 1000. This can be safely ignored
  *  if the event really is less than 1000 meters deep.
  *
  * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



  * @author H. Philip Crotwell
  *
  */
public class TauP_SetSac extends TauP_Time {

   protected Vector sacFileNames = new Vector(10);

   protected boolean evdpkm = false;

   public boolean getEvdpkm() {
      return evdpkm;
   }

   public void setEvdpkm(boolean evdpkm) {
      this.evdpkm = evdpkm;
   }

   public void setSacFileNames(String[] sacFileNames) {
      this.sacFileNames.removeAllElements();
      for (int i=0; i<sacFileNames.length; i++) {
         this.sacFileNames.addElement(sacFileNames[i]);
      }
   }

   protected TauP_SetSac() {
      super();
   }

   public TauP_SetSac(TauModel tMod) throws TauModelException {
      super(tMod);
   }

    public TauP_SetSac(String modelName) throws TauModelException {
        super(modelName);
    }

   protected void setSacVarNums() {
      boolean[] headersUsed = new boolean[10];
      for (int i=0;i<headersUsed.length;i++) {
         headersUsed[i] = false;
      }

      for (int i=0; i<phaseNames.size() && i<10; i++) {
         if (((PhaseName)phaseNames.elementAt(i)).sacTNum != -1) {
            headersUsed[((PhaseName)phaseNames.elementAt(i)).sacTNum] = true;
         }
      }
      int j;
      for (int i=0 ; i<phaseNames.size() ; i++) {
         if (((PhaseName)phaseNames.elementAt(i)).sacTNum == -1) {
                  // find a j that hasn't been used
            for (j=0;j<headersUsed.length && headersUsed[j];j++) {}
            if (j<10) {
               ((PhaseName)phaseNames.elementAt(i)).sacTNum = j;
               headersUsed[j] = true;
            }
         }
      }

   }

   public void calculate(double degrees) {
        recalcPhases();
      calcTime(degrees);
   }

    public void init() throws IOException {
        super.init();
        setSacVarNums();
   }

   public void start() throws IOException, TauModelException {
      SacTimeSeries sacFile = new SacTimeSeries();
      int phaseNum;
      double deg;

      for (int i=0;i<sacFileNames.size();i++) {
         System.out.println((String)sacFileNames.elementAt(i));
         sacFile.read((String)sacFileNames.elementAt(i));
         if (sacFile.evdp == -12345.0f) {
            System.out.println("Depth not set in "+
               (String)sacFileNames.elementAt(i)+", skipping");
            continue;
         }
         if (sacFile.o == -12345.0f) {
            System.out.println("O marker not set in "+
               (String)sacFileNames.elementAt(i)+", skipping");
            continue;
         }

         if (sacFile.gcarc != -12345.0f) {
             if (verbose) {
                 System.out.println("Using gcarc: "+sacFile.gcarc);
             }
            deg = sacFile.gcarc;
         } else if (sacFile.dist != -12345.0f) {
             if (verbose) {
                 System.out.println("Using dist: "+sacFile.dist);
             }
            deg = sacFile.dist/6371.0*180.0/Math.PI;
         } else if (sacFile.stla != -12345.0f && sacFile.stlo != -12345.0f &&
         sacFile.evla != -12345.0f && sacFile.evlo != -12345.0f) {
             if (verbose) {
                 System.out.println("Using stla,stlo, evla,evlo to calculate");
             }
             Alert.warning("Warning: Sac header gcarc is not set,",
                           "using lat and lons to calculate distance.");
             Alert.warning("No ellipticity correction will be applied.",
                          "This may introduce errors. Please see the manual.");
            deg = SphericalCoords.distance(sacFile.stla, sacFile.stlo,
               sacFile.evla, sacFile.evlo);
         } else {
             /* can't get a distance, skipping */
             Alert.warning("Can't get a distance, all distance fields are undef.",
                           "skipping "+(String)sacFileNames.elementAt(i));
             continue;
         }

         if (!((evdpkm && depth == sacFile.evdp) ||
         (!evdpkm && depth == 1000*sacFile.evdp))) {
            if (!evdpkm && sacFile.evdp != 0 && sacFile.evdp < 1000.0) {
               Alert.warning("Sac header evdp is < 1000 in "+
                             (String)sacFileNames.elementAt(i),
                             "If the depth is in kilometers instead of meters "+
                             "(default), you should use the -evdpkm flag");
            }
            if (evdpkm) {
               depthCorrect(sacFile.evdp);
            } else {
               depthCorrect(sacFile.evdp/1000.0);
            }
         }
         if (verbose) {
             System.out.println(sacFileNames.elementAt(i)+
                                " searching for "+getPhaseNameString());
         }
         calculate(deg);
         //         calcTime(deg);
         if (verbose) {
             System.out.println(sacFileNames.elementAt(i)+
                                " "+arrivals.size()+" arrivals found.");
         }
         for (int arrivalNum=arrivals.size()-1;arrivalNum>=0;arrivalNum--) {
            phaseNum = -1;
            for (int j=phaseNames.size()-1;j>=0;j--) {
               if (getArrival(arrivalNum).name.equals(
               ((PhaseName)phaseNames.elementAt(j)).name)) {
                  phaseNum = j;
                  break;
               }
            }
            if (phaseNum != -1) {
               if (verbose) {
                   System.out.println(sacFileNames.elementAt(i)+" phase found "+
                       getArrival(arrivalNum).name+" -> t"+
                       ((PhaseName)phaseNames.elementAt(phaseNum)).sacTNum +
                       ", travel time=" + (float)getArrival(arrivalNum).time);
               }
               setSacTHeader(sacFile, ((PhaseName)phaseNames.elementAt(phaseNum)).sacTNum, getArrival(arrivalNum));
            }
         }
         sacFile.write((String)sacFileNames.elementAt(i));
      }
   }
   
   public static void setSacTHeader(SacTimeSeries sacFile, int headerNum, Arrival arrival) {
       switch(headerNum) {
           case 0:
              sacFile.t0 = sacFile.o + (float)arrival.time;
              sacFile.kt0 = arrival.name;
                sacFile.user0 = (float)arrival.getRayParam();
              break;
           case 1:
              sacFile.t1 = sacFile.o + (float)arrival.time;
              sacFile.kt1 = arrival.name;
                sacFile.user1 = (float)arrival.getRayParam();
              break;
           case 2:
              sacFile.t2 = sacFile.o + (float)arrival.time;
              sacFile.kt2 = arrival.name;
                sacFile.user2 = (float)arrival.getRayParam();
              break;
           case 3:
              sacFile.t3 = sacFile.o + (float)arrival.time;
              sacFile.kt3 = arrival.name;
                sacFile.user3 = (float)arrival.getRayParam();
              break;
           case 4:
              sacFile.t4 = sacFile.o + (float)arrival.time;
              sacFile.kt4 = arrival.name;
                sacFile.user4 = (float)arrival.getRayParam();
              break;
           case 5:
              sacFile.t5 = sacFile.o + (float)arrival.time;
              sacFile.kt5 = arrival.name;
                sacFile.user5 = (float)arrival.getRayParam();
              break;
           case 6:
              sacFile.t6 = sacFile.o + (float)arrival.time;
              sacFile.kt6 = arrival.name;
                sacFile.user6 = (float)arrival.getRayParam();
              break;
           case 7:
              sacFile.t7 = sacFile.o + (float)arrival.time;
              sacFile.kt7 = arrival.name;
                sacFile.user7 = (float)arrival.getRayParam();
              break;
           case 8:
              sacFile.t8 = sacFile.o + (float)arrival.time;
              sacFile.kt8 = arrival.name;
                sacFile.user8 = (float)arrival.getRayParam();
              break;
           case 9:
              sacFile.t9 = sacFile.o + (float)arrival.time;
              sacFile.kt9 = arrival.name;
                sacFile.user9 = (float)arrival.getRayParam();
              break;
           default:
              break;
        }
   }

   public void printStdUsage() {
      String className = this.getClass().getName();
      className =
         className.substring(className.lastIndexOf('.')+1,className.length());

      System.out.println("Usage: "+className.toLowerCase()+" [arguments]");
      System.out.println("  or, for purists, java "+this.getClass().getName()+
         " [arguments]");
      System.out.println("\nArguments are:");

      System.out.println(
         "-ph phase list     -- comma separated phase list,\n"+
         "                      use phase-# to specify the sac header,\n"+
         "                      for example, ScS-8 puts ScS in t8\n"+
         "-pf phasefile      -- file containing phases\n\n"+
         "-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"+
         "                      Default is iasp91.\n\n");
   }

   public void printStdUsageTail() {
    System.out.println(
         "\n-debug             -- enable debugging output\n"+
         "-verbose           -- enable verbose output\n"+
            "-version           -- print the version\n"+
        "-help              -- print this out, but you already know that!\n");
   }

   public void printUsage() {
      printStdUsage();
      System.out.println(
         "-evdpkm            -- sac depth header is in km, default is meters\n");
      printStdUsageTail();
      System.out.println(
         "sacfilename [sacfilename ...]");
      System.out.println("\nEx: taup_setsac "+
         "-mod S_prem -ph S-8,ScS-9 wmq.r wmq.t wmq.z");
      System.out.println("puts the first S arrival in T8 and ScS in T9");
   }

   public String[] parseCmdLineArgs(String[] args) throws IOException {
      int i=0;
      String[] leftOverArgs;
      int numNoComprendoArgs = 0;
      File tempFile;

      leftOverArgs = super.parseCmdLineArgs(args);
      String[] noComprendoArgs = new String[leftOverArgs.length];

      while (i<leftOverArgs.length) {
         if (leftOverArgs[i].equalsIgnoreCase("-evdpkm")) {
            evdpkm = true;
         } else if (leftOverArgs[i].equals("-help")) {
            noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
         } else {
            tempFile = new File(leftOverArgs[i]);
            if (tempFile.exists() && tempFile.isFile() &&tempFile.canRead()) {
               sacFileNames.addElement(leftOverArgs[i]);
            } else {
               noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
            }
         }
         i++;
      }

      if (numNoComprendoArgs > 0) {
         String[] temp = new String[numNoComprendoArgs];
         System.arraycopy(noComprendoArgs,0,temp,0,numNoComprendoArgs);
         return temp;
      } else {
         return new String[0];
      }
   }

     /** Allows TauP_SetSac to run as an application. Creates an instance
       * of TauP_SetSac.
       * . */
   public static void main(String[] args)
      throws FileNotFoundException,
             IOException,
             StreamCorruptedException,
             ClassNotFoundException,
             OptionalDataException
   {
      TauP_SetSac tauPSetSac = new TauP_SetSac();
      if (args.length==0) {
         tauPSetSac.printUsage();
         System.exit(1);
      } else try {

         String[] noComprendoArgs = tauPSetSac.parseCmdLineArgs(args);
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
               if (noComprendoArgs[i].equals("-help") ||
                    noComprendoArgs[i].equals("-version")) {
                  System.out.println();
                  System.exit(0);
               }
            }
            System.out.println();
            noComprendoArgs = null;
         }

         if (tauPSetSac.DEBUG) {
            System.out.println("Done reading "+tauPSetSac.modelName);
         }

         tauPSetSac.init();

         tauPSetSac.start();

      } catch (TauModelException e) {
         System.out.println("Caught TauModelException: "+e.getMessage());
            e.printStackTrace();
      }

   }
}
