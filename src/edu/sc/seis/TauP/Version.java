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

/** convenience class for storing the version of the TauP stuff.
  *
  * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001

  *
  * @author H. Philip Crotwell
  *
  */
public class Version {

    private static int majorNum = 1;

    /** returns the major version number for the package. */
    public static int getMajorNum() {
        return majorNum;
    }

    private static int minorNum = 1;

    /** returns the minor version number for the package. */
    public static int getMinorNum() {
        return minorNum;
    }

    private static int touchNum = 3;

    /** returns the touch version number for the package. The touch number
     *  is changed between versions for every release and is mainly used for
     *  quick bug fixes that only involve special cases or very small sections
     *  of code. */
    public static int getTouchNum() {
        return touchNum;
    }

    /** use date -u to get immediately prior to recompiling. */
    private static String dateCreated = "Wed Jul 18 14:45:43 GMT 2001"; 


    /** returns a string with version number and date created. The format
     *  is "majorNum.minorNum.touchNum [BETA],  date" where the BETA signifies
     *  a beta release and data is the format output by "data -u" on
     *  my Solaris machine, something like "Tue May  5 20:12:55 GMT 1998". */
    public static String getVersion() {
        return majorNum+"."+minorNum+"."+touchNum+",  "+dateCreated;
    }
}
