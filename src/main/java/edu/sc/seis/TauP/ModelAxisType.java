package edu.sc.seis.TauP;

public enum ModelAxisType {

    depth,
    radius,
    velocity,
    velocity_p,
    velocity_s,
    slowness,
    slowness_p,
    slowness_s,
    density,

    velocity_density,
    attenuation_p,
    attenuation_s,
    attenuation,

    poisson,
    shearmodulus,
    lambda,
    bulkmodulus,
    youngsmodulus;

    public static String legendFor(ModelAxisType axisType) {
        switch (axisType) {
            case velocity:
            case velocity_p:
                return "P Vel.";
            case velocity_s:
                return "S Vel.";
            case velocity_density:
            case density:
                return "Density";
            case attenuation:
            case attenuation_p:
                return "P Attenuation";
            case attenuation_s:
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

    public static String labelFor(ModelAxisType axisType) {
        switch (axisType) {
            case velocity_density:
                return "Velocity (km/s), Density (Mg/m3)";
            case velocity:
            case velocity_p:
            case velocity_s:
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
            case attenuation:
                return "P,S Attenuation";
            case attenuation_p:
                return "P Attenuation";
            case attenuation_s:
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
