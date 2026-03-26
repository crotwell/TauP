package edu.sc.seis.TauP;

public enum AxisType {

    radian,
    radian180,
    degree,
    degree180,
    kilometer,
    kilometer180,
    rayparamrad,
    rayparamdeg,
    rayparamkm,
    time,
    tau,
    takeoffangle,
    incidentangle,
    turndepth,
    dpddelta,
    dpddeg,
    amp,
    amppsv,
    ampsh,
    phase,
    phasepsv,
    phasesh,
    phasedeg,
    phasedegpsv,
    phasedegsh,
    unwrapphasedeg,
    unwrapphasedegpsv,
    unwrapphasedegsh,
    geospread,
    refltran,
    refltranpsv,
    refltransh,
    index,
    tstar,
    attenuation,
    theta,
    energygeospread,
    pathlength,
    radiation,
    radiationpsv,
    radiationsh,
    intcaustic;

    public static boolean needsDensity(AxisType axisType) {
        boolean needs;
        switch (axisType) {
            case degree:
            case degree180:
            case kilometer:
            case kilometer180:
            case takeoffangle:
            case incidentangle:
            case radian:
            case radian180:
            case rayparamrad:
            case rayparamdeg:
            case rayparamkm:
            case theta:
            case tau:
            case tstar:
            case time:
            case turndepth:
            case dpddelta:
            case dpddeg:
            case geospread:
            case energygeospread:
            case pathlength:
            case radiation:
            case radiationsh:
            case radiationpsv:
            case attenuation:
            case index:
                needs = false;
                break;
            case amp:
            case amppsv:
            case ampsh:
            case phase:
            case phasepsv:
            case phasesh:
            case phasedeg:
            case phasedegpsv:
            case phasedegsh:
            case unwrapphasedeg:
            case unwrapphasedegpsv:
            case unwrapphasedegsh:
            case intcaustic:
            case refltran:
            case refltranpsv:
            case refltransh:
                needs = true;
                break;
            default:
                throw new RuntimeException("Unknown axis type: "+axisType);
        }
        return needs;
    }

    public static boolean needsQ(AxisType axisType) {
        boolean needs;
        switch (axisType) {
            case degree:
            case degree180:
            case kilometer:
            case kilometer180:
            case takeoffangle:
            case incidentangle:
            case radian:
            case radian180:
            case rayparamrad:
            case rayparamdeg:
            case rayparamkm:
            case theta:
            case tau:
            case time:
            case turndepth:
            case dpddelta:
            case dpddeg:
            case geospread:
            case energygeospread:
            case radiation:
            case radiationpsv:
            case radiationsh:
            case pathlength:
            case index:
            case refltran:
            case refltranpsv:
            case refltransh:
            case phase:
            case phasepsv:
            case phasesh:
            case phasedeg:
            case phasedegpsv:
            case phasedegsh:
            case unwrapphasedeg:
            case unwrapphasedegpsv:
            case unwrapphasedegsh:
            case intcaustic:
                needs = false;
                break;
            case tstar:
            case attenuation:
            case amp:
            case amppsv:
            case ampsh:
                needs = true;
                break;
            default:
                throw new RuntimeException("Unknown axis type: "+axisType);
        }
        return needs;
    }
}
