package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static edu.sc.seis.TauP.SphericalCoords.RtoD;

public class FaultPlane {

    public FaultPlane(double strike, double dip, double rake) {
        this.strike = strike;
        this.dip = dip;
        this.rake = rake;
    }



    public Vector faultNormal() {
        double s = strike*DtoR;
        double d = dip*DtoR;
        double r = rake*DtoR;
        return new Vector(
                Math.sin(d) * Math.cos(s),
                -1 * Math.sin(d) * Math.sin(s),
                Math.cos(d)
        );
    }

    public Vector faultSlip() {
        return faultVector(rake);
    }

    /**
     * A vector within the fault plane at the given rake/slip angle.
     * @param rake rake/slip angle
     * @return vector in plane
     */
    public Vector faultVector(double rake) {
        double s = strike*DtoR;
        double d = dip*DtoR;
        double r = rake*DtoR;
        return new Vector(
                Math.cos(r)*Math.sin(s)-Math.sin(r) * Math.cos(d) * Math.cos(s),
                Math.sin(r) * Math.cos(d) * Math.sin(s)+Math.cos(r)*Math.cos(s),
                Math.sin(r)*Math.sin(d)
        );
    }

    public FaultPlane auxPlane() {
        Vector normal = faultSlip();
        Vector slip = faultNormal();
        double dip = Math.acos(normal.z);
        double sinDip = Math.sin(dip);
        double strike = Math.atan2(-1*normal.y/sinDip, normal.x/sinDip);
        double rake = Math.asin(slip.z/sinDip);
        FaultPlane auxPlane = new FaultPlane(strike*RtoD, dip*RtoD, rake*RtoD);
        return auxPlane;
    }

    public Vector nullAxis() {
        return Vector.crossProduct(faultNormal(), faultSlip());
    }

    public Vector pAxis() {
        Vector n = faultNormal();
        Vector d = faultSlip();
        Vector p = n.plus(d).normalize();
        return p;
    }

    public Vector tAxis() {
        Vector n = faultNormal();
        Vector d = faultSlip();
        Vector t = n.minus(d).normalize();
        return t;
    }

    double strike;
    double dip;
    double rake;
}
