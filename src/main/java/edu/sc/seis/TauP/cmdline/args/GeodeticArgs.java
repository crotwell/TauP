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

    @CommandLine.Option(names= "--geodeticflattening",
            paramLabel = "f",
            description = "Inverse Elliptical flattening for distance calculations when --geodetic, "
                    +"defaults to WGS84 ~ 298.257. The distance calculation uses 1/x.")
    protected double geodeticFlattening = DistAz.wgs85_invflattening;

    public double getInverseEllipFlattening() {
        return geodeticFlattening;
    }
}
