package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

public enum DistAzPlanet {

    EARTH (Geodesic.WGS84.EquatorialRadius(), Geodesic.WGS84.Flattening()),
    MOON (1738.1*1000, 1.0/170),
    MARS (3396.19*1000, 0.0012),
    VENUS (6051.8*1000, 0.0),
    MERCURY (2439.7*1000, 0.0009),
    CERES (964.3*1000, 1.0/13.3),
    JUPITER (142984*1000, 1.0/15.41),
    SATURN (120536*1000, 1.0/10.21),
    URANUS (51118*1000, 1.0/43.62),
    NEPTUNE (49528*1000, 1.0/58.54);

    private final double equitorialradius;   // in kilograms
    private final double geodeticFlattening; // in meters

    DistAzPlanet(double equitorialradius, double geodeticFlattening) {
        this.equitorialradius = equitorialradius;
        this.geodeticFlattening = geodeticFlattening;
    }

    private double equitorialradius() { return equitorialradius; }

    private double geodeticFlattening() { return geodeticFlattening; }

    public Geodesic createGeodesic() {
        return new Geodesic(equitorialradius, geodeticFlattening);
    }
}
