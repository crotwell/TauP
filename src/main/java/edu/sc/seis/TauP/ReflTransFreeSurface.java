package edu.sc.seis.TauP;

public abstract class ReflTransFreeSurface extends ReflTrans {

    ReflTransFreeSurface(double topVp, double topVs, double topDensity, double botVp, double botVs, double botDensity) {
        super(topVp, topVs, topDensity, botVp, botVs, botDensity);
    }

    public static ReflTransFreeSurface createReflTransFreeSurface(double topVp, double topVs, double topDensity) throws VelocityModelException {
        if (topVs != 0) {
            return new ReflTransSolidFreeSurface(topVp, topVs, topDensity);
        } else {
            return new ReflTransFluidFreeSurface(topVp, topDensity);
        }
    }

    public abstract double getFreeSurfaceReceiverFunP_r(double rayParam);

    public abstract double getFreeSurfaceReceiverFunP_z(double rayParam);

    public abstract Complex[] getFreeSurfaceReceiverFunP(double rayParam);

    // no-ops for S, always 0
    public abstract double getFreeSurfaceReceiverFunSv_r(double rayParam);

    public abstract double getFreeSurfaceReceiverFunSv_z(double rayParam);

    public abstract Complex[] getFreeSurfaceReceiverFunSv(double rayParam);

    public abstract double getFreeSurfaceReceiverFunSh(double rayParam);
}
