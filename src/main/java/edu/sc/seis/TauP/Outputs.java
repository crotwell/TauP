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

import edu.sc.seis.TauP.cmdline.TauP_ReflTransPlot;

import java.util.Locale;
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

    /**
     * Gets appropriate format spec for a given data type. Usually one of ModelAxisType or AxisType.
     * @param axisType data type
     * @return output format like %3.2f for use in formatting floats
     */
    public static String formatStringForAxisType(String axisType) {
        // TauPReflTrans.DegRayParam
        if (axisType.equals(TauP_ReflTransPlot.DegRayParam.degree.name())) {
            return "%8.3f";
        } else if (axisType.equals(TauP_ReflTransPlot.DegRayParam.rayparam.name())) {
            return "%8.5f";
        }
        try {
            AxisType at = AxisType.valueOf(axisType);
            return formatStringForAxisType(at);
        } catch (IllegalArgumentException e ) {
            try {
                ModelAxisType mt = ModelAxisType.valueOf(axisType);
                return formatStringForAxisType(mt);
            } catch (IllegalArgumentException ee ) {
                try {
                    ReflTransAxisType mt = ReflTransAxisType.valueOf(axisType);
                    return formatStringForAxisType(mt);
                } catch (IllegalArgumentException eee ) {
                    Alert.warning("Unknown axis type: " + axisType);
                    return "%f";
                }
            }
        }
    }

    public static String formatStringForAxisType(ModelAxisType axisType) {
        String outFormat;
        switch (axisType) {
            case depth:
            case radius:
                outFormat = depthFormat;
                break;
            case velocity:
            case Vp:
            case Vs:
            case vpvs:
            case vpdensity:
            case vsdensity:
            case velocity_density:
                outFormat = rayParamFormat;
                break;
            case youngsmodulus:
            case poisson:
            case shearmodulus:
            case bulkmodulus:
            case lambda:
            case slownessdeg:
            case slownessrad:
            case slownessdeg_p:
            case slownessdeg_s:
            case slownessrad_p:
            case slownessrad_s:
                outFormat = rayParamFormat;
                break;
            default:
                outFormat = "%f";
                break;
        }
        return outFormat;
    }

    public static String formatStringForAxisType(AxisType axisType) {
        String outFormat;
        switch (axisType) {
            case degree:
            case degree180:
            case kilometer:
            case kilometer180:
            case takeoffangle:
            case incidentangle:
                outFormat = distanceFormat;
                break;
            case radian:
            case radian180:
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

    public static String formatStringForAxisType(ReflTransAxisType axisType) {
        return "%f";
    }

    public static String formatDepth(double depth) {

        return String.format(Locale.ROOT, depthFormat, depth);
    }

    public static String formatDistance(double distance) {

        return String.format(Locale.ROOT,  distanceFormat, distance);
    }

    public static String formatKilometer(double kilometers) {
        return String.format(Locale.ROOT,  depthFormat, kilometers);
    }

    public static String formatTime(double time) {

        return String.format(Locale.ROOT,  timeFormat, time);
    }

    public static String formatDistanceNoPad(double distance) {

        return String.format(Locale.ROOT,  distanceFormatNoPad, distance);
    }

    public static String formatTimeNoPad(double time) {

        return String.format(Locale.ROOT,  timeFormat, time);
    }

    public static String formatRayParam(double rayParam) {

        return String.format(Locale.ROOT,  rayParamFormat, rayParam);
    }

    public static String formatLatLon(double latlon) {

        return String.format(Locale.ROOT, latLonFormat, latlon);
    }

    public static String formatAmpFactor(double ampFactor) {
        String space = " ";
        if (ampFactor < 0) { space = "";}
        if (ampFactor == 0) {
            // so not extra minus in -0.0e+00
            return space+String.format(Locale.ROOT, ampFactorFormat, 0.0);
        }
        return space+String.format(Locale.ROOT, ampFactorFormat, ampFactor);
    }

    public static String depthFormat = "%8.1f";

    public static String distanceFormat = "%8.2f";

    public static String timeFormat = "%8.2f";

    public static String distanceFormatNoPad = "%.2f";

    public static String timeFormatNoPad = "%.2f";

    public static String rayParamFormat = "%8.3f";

    public static String latLonFormat = "%8.2f";

    public static String ampFactorFormat = "%.1e";

} // Outputs
