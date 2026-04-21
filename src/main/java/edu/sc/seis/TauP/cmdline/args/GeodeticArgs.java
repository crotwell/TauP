package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.*;
import net.sf.geographiclib.Geodesic;
import picocli.CommandLine;

import java.util.*;

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

    // legacy opt for --geodetic
    @CommandLine.Option(names = "--geodetic",
            description = "use geodetic latitude for distance calculations, which implies an ellipticity. "
                    + "Default is spherical. Note this only affects calculation of distance from lat/lon pairs, "
                    + "all travel time calculations are done in a purely spherical model.")
    boolean with_geodetic = false;
    public void setGeodetic(boolean geodetic) {
        with_geodetic = geodetic;
    }

    @CommandLine.Option(names = {"--geodist"},
            description = "Type of distance calculation to use for lat,lon distance calculation, "
                    +"one of ${COMPLETION-CANDIDATES}. "
                    + "Default is spherical. Note this only affects calculation of distance from lat/lon pairs, "
                    + "all travel time calculations are done in a purely spherical model.",
            arity = "1..*",
            defaultValue = "spherical"
    )
    public void setGeoDistTypes(List<GeoDistType> geoDistTypes) {
        this.geoDistTypeList = geoDistTypes;
    }
    public List<GeoDistType> getGeoDistTypes() {
        if (this.geoDistTypeList.isEmpty()) {
            if (with_geodetic) {
                this.geoDistTypeList.add(GeoDistType.geodetic);
            } else {
                this.geoDistTypeList.add(GeoDistType.spherical);
            }
        }
        return this.geoDistTypeList;
    }

    protected List<GeoDistType> geoDistTypeList = new ArrayList<>();

    @CommandLine.Option(names = {"--invflattening", "--geodeticflattening"},
            paramLabel = "f",
            description = "Inverse Elliptical flattening for distance calculations when geocentric or geodetic, "
                    + "defaults to WGS84 ~ 298.257. The distance calculation uses 1/x.")
    protected Double geodeticFlattening = null;


    public double getEllipFlattening() {
        if (geodeticFlattening != null) {
            return 1.0/geodeticFlattening;
        } else if (planet != null) {
            return planet.createGeodesic().Flattening();
        }
        return Geodesic.WGS84.Flattening();
    }
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
            description = "Equitorial radius in meters for distance calculations when geodetic or geocentric, "
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
    public void setPlanet(DistAzPlanet planet) {
        this.planet = planet;
    }
    protected DistAzPlanet planet = null;

    public void validateArguments() {
        if (geodeticFlattening != null && getInverseEllipFlattening() <= 0) {
            throw new IllegalArgumentException("Inverse Elliptical flattening must be positive: " + getInverseEllipFlattening());
        }
        if (planet != null && (equitorialradius != null && geodeticFlattening != null)) {
            throw new IllegalArgumentException("Cannot specify --planet and either of --equitorialradius or --geodeticflattening");
        }
    }

    public Map<GeoDistType, Geodesic>  createGeodesics(VelocityModel vMod) {
        return createGeodesics(getGeoDistTypes(), vMod);
    }

    public Geodesic createGeodesic(GeoDistType geoDistType, VelocityModel vMod) {
        if (geoDistType == GeoDistType.spherical) {
            return vMod.sphericalGeodesic();
        } else {
            double f = getEllipFlattening();
            return new Geodesic(DistAzKarney.equitorialRadius(vMod.getRadiusOfEarthMeters(), f), f);
        }
    }
    public Map<GeoDistType, Geodesic> createGeodesics(List<GeoDistType> geoDistTypes, VelocityModel vMod) {
        Map<GeoDistType, Geodesic> geodesicList = new HashMap<>();
        for (GeoDistType gdt : geoDistTypes) {
            geodesicList.put(gdt, createGeodesic(gdt, vMod));
        }
        return geodesicList;
    }

    public Geodesic getGeodesic(GeoDistType geoDistType) {
        Geodesic geodesic = getGeodesic();
        if (geoDistType == GeoDistType.spherical) {
            return new Geodesic(DistAzKarney.averageRadiusMeter(geodesic), 0);
        }
        return geodesic;
    }
    public Geodesic getGeodesic() {
        Geodesic geodesic = Geodesic.WGS84;
        if (planet != null) {
            geodesic = planet.createGeodesic();
        } else if (equitorialradius == null && geodeticFlattening == null) {
            return Geodesic.WGS84;
        } else {
            geodesic = new Geodesic(equitorialradius != null ? equitorialradius : Geodesic.WGS84.EquatorialRadius(),
                    geodeticFlattening!= null ? 1.0/geodeticFlattening : Geodesic.WGS84.Flattening());
        }
        return geodesic;
    }

}
