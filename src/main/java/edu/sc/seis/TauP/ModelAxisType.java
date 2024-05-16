package edu.sc.seis.TauP;

public enum ModelAxisType {

    depth,
    radius,
    velocity,
    Vp,
    Vs,
    slowness,
    slowness_p,
    slowness_s,
    density,

    velocity_density,
    Qp,
    Qs,
    Q,

    poisson,
    shearmodulus,
    lambda,
    bulkmodulus,
    youngsmodulus;

    public static String legendFor(ModelAxisType axisType) {
        switch (axisType) {
            case velocity:
            case Vp:
                return "P Vel.";
            case Vs:
                return "S Vel.";
            case velocity_density:
            case density:
                return "Density";
            case Q:
            case Qp:
                return "Qp Attenuation";
            case Qs:
                return "Qs Attenuation";
            case poisson:
                return "Poisson's Ratio";
            case lambda:
                return "Lambda";
            case bulkmodulus:
                return "Bulk Modulus";
            case shearmodulus:
                return "Shear Modulus";
            case youngsmodulus:
                return "Young's Modulus";
            default:
                return axisType.name();
        }
    }

    public static String labelFor(ModelAxisType axisType) {
        switch (axisType) {
            case velocity_density:
                return "Velocity (km/s), Density (Mg/m3)";
            case velocity:
            case Vp:
            case Vs:
                return "Velocity (km/s)";
            case depth:
                return "Depth (km)";
            case radius:
                return "Radius (km)";
            case density:
                return "Density (Mg/m3)";
            case slowness:
            case slowness_p:
            case slowness_s:
                return "Slowness (s/rad)";
            case Q:
                return "P,S Attenuation";
            case Qp:
                return "P Attenuation";
            case Qs:
                return "S Attenuation";
            case poisson:
                return "Poisson's Ratio";
            case lambda:
                return "Lambda";
            case bulkmodulus:
                return "Bulk Modulus";
            case shearmodulus:
                return "Shear Modulus";
            case youngsmodulus:
                return "Young's Modulus";
            default:
                return axisType.name();
        }
    }
}
