package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.DistAz;
import picocli.CommandLine;

public class GeodeticArgs {

    public boolean isGeodetic() {
        return geodetic;
    }

    @CommandLine.Option(names = "--geodetic",
            description = "use geodetic latitude for distance calculations, which implies an ellipticity. "
                    +"Default is spherical. Note this only affects calculation of distance from lat/lon pairs, "
                    +"all travel time calculations are done in a purely spherical model.")
    protected boolean geodetic = false;

    @CommandLine.Option(names= "--ellipflattening",
            paramLabel = "f",
            description = "Elliptical flattening for distance calculations when --geodetic, "
                    +"defaults to WGS84 ~ 1/298.257")
    protected double ellipflattening = DistAz.wgs85_flattening;

    public double getEllipFlattening() {
        return ellipflattening;
    }
}
