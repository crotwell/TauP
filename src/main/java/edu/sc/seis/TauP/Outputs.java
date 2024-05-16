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

import java.util.Properties;

/**
 * Outputs.java contains formating, similar to printf, routines for the output
 * types in the TauP package.
 * Created: Tue Sep 21 11:45:35 1999
 * 
 * @author Philip Crotwell
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 */
public class Outputs {
    
    public static void configure(Properties props) {
        depthFormat = "%8." + props.getProperty("taup.depth.precision", "1")
                + "f";
        distanceFormat = "%8." + props.getProperty("taup.distance.precision", "2")
                + "f";
        timeFormat = "%8." + props.getProperty("taup.time.precision", "2")
                + "f";
        distanceFormatNoPad = "%." + props.getProperty("taup.distance.precision", "2")
                + "f";
        timeFormatNoPad = "%." + props.getProperty("taup.time.precision", "2")
                + "f";
        rayParamFormat = "%8." + props.getProperty("taup.rayparam.precision", "3")
                + "f";
        latLonFormat = "%8." + props.getProperty("taup.latlon.precision", "2")
                + "f";
        ampFactorFormat = "%." + props.getProperty("taup.amplitude.precision", "1")
                + "e";
    }

    public static String formatStringForAxisType(String axisType) {
        try {
            AxisType at = AxisType.valueOf(axisType);
            return formatStringForAxisType(at);
        } catch (IllegalArgumentException e ) {
            System.err.println("Unknown axis type: "+axisType);
            return "%f";
        }
    }

    public static String formatStringForAxisType(AxisType axisType) {
        String outFormat;
        switch (axisType) {
            case radian:
            case radian180:
            case degree:
            case degree180:
            case kilometer:
            case kilometer180:
                outFormat = distanceFormat;
                break;
            case rayparamrad:
            case rayparamdeg:
            case rayparamkm:
            case theta:
            case tau:
            case tstar:
                outFormat = rayParamFormat;
                break;
            case time:
                outFormat = timeFormat;
                break;
            case amp:
            case amppsv:
            case ampsh:
            case geospread:
            case refltran:
            case refltranpsv:
            case refltransh:
            case attenuation:
                outFormat = ampFactorFormat;
                break;
            case turndepth:
                outFormat = distanceFormatNoPad;
                break;
            case index:
                outFormat = "%8.0f";
                break;
            default:
                outFormat = "%f";
                break;
        }
        return outFormat;
    }

    public static String formatDepth(double depth) {

        return String.format( depthFormat, depth);
    }

    public static String formatDistance(double distance) {

        return String.format( distanceFormat, distance);
    }

    public static String formatTime(double time) {

        return String.format( timeFormat, time);
    }

    public static String formatDistanceNoPad(double distance) {

        return String.format( distanceFormatNoPad, distance);
    }

    public static String formatTimeNoPad(double time) {

        return String.format( timeFormat, time);
    }

    public static String formatRayParam(double rayParam) {

        return String.format( rayParamFormat, rayParam);
    }

    public static String formatLatLon(double latlon) {

        return String.format(latLonFormat, latlon);
    }

    public static String formatAmpFactor(double ampFactor) {
        String space = " ";
        if (ampFactor < 0) { space = "";}
        if (ampFactor == 0) {
            // so not extra minus in -0.0e+00
            return space+String.format(ampFactorFormat, 0.0);
        }
        return space+String.format(ampFactorFormat, ampFactor);
    }

    protected static String depthFormat = "%8.1f";

    protected static String distanceFormat = "%8.2f";

    protected static String timeFormat = "%8.2f";

    protected static String distanceFormatNoPad = "%.2f";

    protected static String timeFormatNoPad = "%.2f";

    protected static String rayParamFormat = "%8.3f";

    protected static String latLonFormat = "%8.2f";

    protected static String ampFactorFormat = "%.1e";
} // Outputs
