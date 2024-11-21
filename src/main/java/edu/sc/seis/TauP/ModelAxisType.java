package edu.sc.seis.TauP;

public enum ModelAxisType {

    depth,
    radius,
    velocity,
    Vp,
    Vs,
    slownessdeg,
    slownessdeg_p,
    slownessdeg_s,
    slownessrad,
    slownessrad_p,
    slownessrad_s,
    density,

    velocity_density,
    Qp,
    Qs,
    Q,

    vpvs,
    vpdensity,
    vsdensity,
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
            case slownessdeg:
            case slownessdeg_p:
            case slownessrad:
            case slownessrad_p:
                return "P Slowness";
            case slownessrad_s:
            case slownessdeg_s:
                return "S Slowness";
            case Q:
            case Qp:
                return "Qp Attenuation";
            case Qs:
                return "Qs Attenuation";
            case vpvs:
                return "Vp/Vs Ratio";
            case vpdensity:
                return "Vp/Density Ratio";
            case vsdensity:
                return "Vs/Density Ratio";
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
                return "Velocity (km/s), Density (Mg/m³)";
            case velocity:
            case Vp:
            case Vs:
                return "Velocity (km/s)";
            case depth:
                return "Depth (km)";
            case radius:
                return "Radius (km)";
            case density:
                return "Density (Mg/m³)";
            case slownessdeg:
            case slownessdeg_p:
            case slownessdeg_s:
                return "Slowness (s/deg)";
            case slownessrad:
            case slownessrad_p:
            case slownessrad_s:
                return "Slowness (s/rad)";
            case Q:
                return "Qp,Qs Attenuation";
            case Qp:
                return "Qp Attenuation";
            case Qs:
                return "Qs Attenuation";
            case vpvs:
                return "Vp/Vs Ratio";
            case vpdensity:
                return "Vp/Density Ratio";
            case vsdensity:
                return "Vs/Density Ratio";
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


    public static boolean needsDensity(ModelAxisType axisType) {
        switch (axisType) {
            case velocity_density:
            case density:
            case vpdensity:
            case vsdensity:
            case lambda:
            case bulkmodulus:
            case shearmodulus:
            case youngsmodulus:
                return true;
            default:
                return false;
        }
    }

    public static boolean needsQ(ModelAxisType axisType) {
        switch (axisType) {
            case Q:
            case Qp:
            case Qs:
                return true;
            default:
                return false;
        }
    }
}
