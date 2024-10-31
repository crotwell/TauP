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

/**
 * Displays a message to the user depending on whether there is a GUI or not.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 * 
 */
public class Alert {

    /** the machine/OS dependent line terminator, ie \r \r\n or \n. */
    static String nl = System.getProperty("line.separator");

    /** Whether or not a GUI is being used. */
    static private boolean GUI = false;

    private Alert() {}

    /*
     * reports information in a manner consistant with the current usage, ie GUI
     * or command line.
     */
    public static void info(String message) {
        if(GUI) {
            System.out.println(message);
        } else {
            System.out.println(message);
        }
    }

    /*
     * reports non fatal errors in a manner consistant with the current usage,
     * ie GUI or command line.
     */
    public static void warning(String message, String extra) {
        String nl_extra = (extra != null && extra.length()>0) ?  (nl + extra) : "";
        if(GUI) {
            System.err.println("Warning: " + message + nl_extra);
        } else {
            System.err.println("Warning: " + message + nl_extra);
        }
    }
    public static void warning(String message) {
        warning(message, null);
    }

    /*
     * reports fatal errors in a manner consistant with the current usage, ie
     * GUI or command line.
     */
    public static void error(String message, String extra) {
        if(GUI) {
            System.err.println("Error: " + message + nl + extra);
        } else {
            System.err.println("Error: " + message + nl + extra);
        }
    }

    /**
     * Simple proxy method so don't have System.err.println scattered in code.
     * @param message message to print
     */
    public static void debug(String message) {
        System.err.println(message);
    }

    public static void setGUI(boolean newGUI) {
        GUI = newGUI;
    }

    public static void warning(Exception e) {
        warning("" +e);
    }
}
