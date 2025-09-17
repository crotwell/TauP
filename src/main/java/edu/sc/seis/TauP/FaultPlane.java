package edu.sc.seis.TauP;

import java.util.Objects;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static edu.sc.seis.TauP.SphericalCoords.RtoD;

public class FaultPlane {

    public FaultPlane(double strike, double dip, double rake) {
        this.strike = strike;
        this.dip = dip;
        this.rake = rake;
    }

    public FaultPlane(edu.sc.seis.seisFile.fdsnws.quakeml.NodalPlane qmlNodalPlane) {
        this(qmlNodalPlane.getStrike().getValue(),
                qmlNodalPlane.getDip().getValue(),
                qmlNodalPlane.getRake().getValue());
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
        Vector b = Vector.crossProduct(faultNormal(), faultSlip());
        if (b.z > 0) {
            b = b.negate();
        }
        return b;
    }

    public Vector pAxis() {
        Vector n = faultNormal();
        Vector d = faultSlip();
        Vector p = n.plus(d).normalize();
        if (p.z > 0) {
            p = p.negate();
        }
        return p;
    }

    public Vector tAxis() {
        Vector n = faultNormal();
        Vector d = faultSlip();
        Vector t = n.minus(d).normalize();
        if (t.z > 0) {
            t = t.negate();
        }
        return t;
    }



    /**
     * Calculate radiation pattern terms, Fp, Fsv, Fsh for the given fault orientation and az,takeoff.
     *
     * @param azimuth azimuth to receiver in degrees
     * @param takeoff takeoff angle in degrees
     * @return  Fp, Fsv, Fsh
     */
    public double[] calcRadiationPatDegree(double azimuth, double takeoff) {
        return calcRadiationPatRadian(azimuth*DtoR, takeoff*DtoR);
    }

    /**
     * Calculate radiation pattern terms, Fp, Fsv, Fsh for the given fault orientation and az,takeoff.
     * ALl in radians.
     * @param azimuth azimuth to receiver in radian
     * @param takeoff takeoff angle in radian
     * @return  Fp, Fsv, Fsh
     */
    public double[] calcRadiationPatRadian(double azimuth, double takeoff) {
        double ih = takeoff;
        double phi_f = strike*DtoR;
        double phi_r = azimuth;
        double phi_r_f = phi_r - phi_f;
        double theta = dip*DtoR;
        double lam = rake*DtoR;
        double Fp = (Math.cos(lam)*Math.sin(theta)*Math.sin(2*phi_r_f)
                - Math.sin(lam)*Math.sin(2*theta)*Math.sin(phi_r_f)*Math.sin(phi_r_f)
        )*Math.sin(ih)*Math.sin(ih)
                + (Math.sin(lam)*Math.cos(2*theta)*Math.sin(phi_r_f)
                - Math.cos(lam)*Math.cos(theta)*Math.cos(phi_r_f)
        )*Math.sin(2*ih)
                + Math.sin(lam)*Math.sin(2*theta)*Math.cos(ih)*Math.cos(ih);

        double Fsv = (Math.sin(lam)*Math.cos(2*theta)*Math.sin(phi_r_f)
                - Math.cos(lam)*Math.cos(theta)*Math.cos(phi_r_f)) * Math.cos(2*ih)
                + 1.0/2*Math.cos(lam)*Math.sin(theta)*Math.sin(2*phi_r_f)*Math.sin(2*ih)
                - 1.0/2*Math.sin(lam)*Math.sin(2*theta)*Math.sin(2*ih)*(1 + Math.sin(phi_r_f)*Math.sin(phi_r_f));

        double Fsh = (Math.cos(lam)*Math.cos(theta)*Math.sin(phi_r_f)
                + Math.sin(lam)*Math.cos(2*theta)*Math.cos(phi_r_f))*Math.cos(ih)
                +(Math.cos(lam)*Math.sin(theta)*Math.cos(2*phi_r_f)
                - 1.0/2*Math.sin(lam)*Math.sin(2*theta)*Math.sin(2*phi_r_f))*Math.sin(ih);
        return new double[] {Fp, Fsv, Fsh};
    }

    public double getStrike() {
        return strike;
    }

    public double getDip() {
        return dip;
    }

    public double getRake() {
        return rake;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FaultPlane)) return false;
        FaultPlane that = (FaultPlane) o;
        return Double.compare(strike, that.strike) == 0 && Double.compare(dip, that.dip) == 0 && Double.compare(rake, that.rake) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(strike, dip, rake);
    }

    double strike;
    double dip;
    double rake;
}
