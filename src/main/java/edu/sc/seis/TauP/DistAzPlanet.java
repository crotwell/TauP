package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

public enum DistAzPlanet {

    EARTH (Geodesic.WGS84.EquatorialRadius(), Geodesic.WGS84.Flattening(), Ellipticipy.EARTH_LOD),
    MOON (1738.1*1000, 1.0/170, 27.321661*86400),
    MARS (3396.19*1000, 0.0012, 1.025957*86400),
    VENUS (6051.8*1000, 0.0, -243.0226*86400),
    MERCURY (2439.7*1000, 0.0009, 1407.5*3600),
    CERES (964.3*1000, 1.0/13.3, 9.074170*3600),
    JUPITER (142984*1000, 1.0/15.41, 9.9250*3600),
    SATURN (120536*1000, 1.0/10.21, 10*3600+33*60+38),
    URANUS (51118*1000, 1.0/43.62, -0.718661*86400),
    NEPTUNE (49528*1000, 1.0/58.54, 0.673 *86400),
    PLUTO (1188.3*1000, 0.0, -6.387222*86400);

    private final double equitorialradius;   // in kilograms
    private final double geodeticFlattening; // in meters
    private final double siderealDay; // in seconds

    DistAzPlanet(double equitorialradius, double geodeticFlattening, double siderealDay) {
        this.equitorialradius = equitorialradius;
        this.geodeticFlattening = geodeticFlattening;
        this.siderealDay = siderealDay;
    }

    private double equitorialradius() { return equitorialradius; }

    private double geodeticFlattening() { return geodeticFlattening; }

    public Geodesic createGeodesic() {
        return new Geodesic(equitorialradius, geodeticFlattening);
    }

    public double getSiderealDay() {return siderealDay;}
}
