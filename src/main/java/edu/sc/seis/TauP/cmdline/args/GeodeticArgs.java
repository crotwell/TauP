package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.DistAz;
import edu.sc.seis.TauP.DistAzPlanet;
import net.sf.geographiclib.Geodesic;
import picocli.CommandLine;

public class GeodeticArgs extends LatLonArgs {

    @CommandLine.Option(names = "--az", description = "azimuth in degrees, source to receiver")
    protected Double azimuth = null;

    public boolean hasAzimuth() {
        return azimuth != null;
    }
    public Double getAzimuth() {
        return azimuth;
    }

    @CommandLine.Option(names = "--baz", description = "backazimuth in degrees, receiver to source")
    protected Double backAzimuth = null;

    public boolean hasBackAzimuth() {
        return backAzimuth != null;
    }

    public Double getBackAzimuth() {
        return backAzimuth;
    }

    public boolean isGeodetic() {
        return geodetic;
    }

    @CommandLine.Option(names = "--geodetic",
            description = "use geodetic latitude for distance calculations, which implies an ellipticity. "
                    + "Default is spherical. Note this only affects calculation of distance from lat/lon pairs, "
                    + "all travel time calculations are done in a purely spherical model.")
    protected boolean geodetic = false;

    @CommandLine.Option(names = {"--invflattening", "--geodeticflattening"},
            paramLabel = "f",
            description = "Inverse Elliptical flattening for distance calculations when --geodetic, "
                    + "defaults to WGS84 ~ 298.257. The distance calculation uses 1/x.")
    protected Double geodeticFlattening = null;

    public double getInverseEllipFlattening() {
        if (geodeticFlattening != null) {
            return geodeticFlattening;
        } else if (planet != null) {
            return 1.0/planet.createGeodesic().Flattening();
        }
        return 1.0 / Geodesic.WGS84.Flattening();
    }


    @CommandLine.Option(names = "--equitorialradius",
            paramLabel = "m",
            description = "Equitorial radius in meters for distance calculations when --geodetic, "
                    + "defaults to WGS84 ~ 6378137 meters. ")
    protected Double equitorialradius = null;

    public double getEquitorialRadius() {
        if (equitorialradius != null) {
            return equitorialradius;
        } else if (planet != null) {
            return planet.createGeodesic().EquatorialRadius();
        }
        return Geodesic.WGS84.EquatorialRadius();
    }

    @CommandLine.Option(names = "--planet",
            description = "Geodesic for planets, for distance calculations when --geodetic"
                    +", one of: ${COMPLETION-CANDIDATES}. ")
    protected DistAzPlanet planet = null;

    public void validateArguments() {
        if (geodeticFlattening != null && getInverseEllipFlattening() <= 0) {
            throw new IllegalArgumentException("Inverse Elliptical flattening must be positive: " + getInverseEllipFlattening());
        }
        if (planet != null && (equitorialradius != null && geodeticFlattening != null)) {
            throw new IllegalArgumentException("Cannot specify --planet and either of --equitorialradius or --geodeticflattening");
        }
    }

    public Geodesic getGeodesic() {
        Geodesic geodesic = Geodesic.WGS84;
        if (geodetic) {
            if (planet != null) {
                geodesic = planet.createGeodesic();
            } else {
                geodesic = new Geodesic(equitorialradius != null ? equitorialradius : Geodesic.WGS84.EquatorialRadius(),
                        geodeticFlattening!= null ? 1.0/geodeticFlattening : Geodesic.WGS84.Flattening());
            }
        } else {
            // flattening of zero is sphere
            geodesic = new Geodesic(equitorialradius, 0.0);
        }
        return geodesic;
    }

    public String getCalcType() {
        if (geodetic) {
            return GEODETIC;
        } else {
            return SPHERICAL;
        }
    }

    public static final String GEODETIC = "geodetic";
    public static final String SPHERICAL = "spherical";
}
