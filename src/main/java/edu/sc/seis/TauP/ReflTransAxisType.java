package edu.sc.seis.TauP;

import java.util.List;

public enum ReflTransAxisType {
    Rpp,
    Rps,
    Rsp,
    Rss,
    Tpp,
    Tps,
    Tsp,
    Tss,
    Rshsh,
    Tshsh,
    RppEnergy, TppEnergy, RpsEnergy, TpsEnergy,
    RspEnergy, TspEnergy, RssEnergy, TssEnergy,
    RshshEnergy, TshshEnergy, RpAngle,
    RsAngle, TpAngle, TsAngle,
    FreeRecFuncPr, FreeRecFuncSvr, FreeRecFuncPz, FreeRecFuncSvz, FreeRecFuncSh;

    public static final List<ReflTransAxisType> inpwave = List.of(Rpp, Rps, Tpp, Tps);
    public static final List<ReflTransAxisType> insvwave = List.of(Rsp, Rss, Tsp, Tss);
    public static final List<ReflTransAxisType> allSh = List.of(Rshsh, Tshsh);
    public static final List<ReflTransAxisType> allPSv = List.of(Rpp, Rps, Tpp, Tps, Rsp, Rss, Tsp, Tss);

    public static final List<ReflTransAxisType> allDisplacement = List.of(Rpp, Rps, Tpp, Tps, Rsp, Rss, Tsp, Tss, Rshsh, Tshsh);
    public static final List<ReflTransAxisType> allFreeRF = List.of(FreeRecFuncPr, FreeRecFuncSvr,
            FreeRecFuncPz, FreeRecFuncSvz, FreeRecFuncSh);


    public static final List<ReflTransAxisType> allEnergy = List.of(
            RppEnergy, TppEnergy, RpsEnergy, TpsEnergy,
            RspEnergy, TspEnergy, RssEnergy, TssEnergy,
            RshshEnergy, TshshEnergy);

    public static String labelFor(ReflTransAxisType axisType) {
        switch (axisType) {
            case Rpp:
            case Rps:
            case Tpp:
            case Tps:
            case Rsp:
            case Rss:
            case Tsp:
            case Tss:
            case Rshsh:
            case Tshsh:
                return axisType.name()+" Displacement";
            case RppEnergy:
            case RpsEnergy:
            case TppEnergy:
            case TpsEnergy:
            case RspEnergy:
            case RssEnergy:
            case TspEnergy:
            case TssEnergy:
            case RshshEnergy:
            case TshshEnergy:
                return axisType.name().replace("Energy", " Energy");
            case FreeRecFuncPr:
            case FreeRecFuncPz:
            case FreeRecFuncSvr:
            case FreeRecFuncSvz:
            case FreeRecFuncSh:
                return "Free Surface Factor "+axisType.name().replace("FreeRecFunc", "");
            case RpAngle:
            case RsAngle:
            case TpAngle:
            case TsAngle:
                return axisType.name().replace("Angle", " Angle");
            default:
                return axisType.name();
        }
    }

}
