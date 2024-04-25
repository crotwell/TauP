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
    RsAngle, TpAngle, TsAngle;

    public static final List<ReflTransAxisType> inpwave = List.of(Rpp, Rps, Tpp, Tps);
    public static final List<ReflTransAxisType> insvwave = List.of(Rsp, Rss, Tsp, Tss);
    public static final List<ReflTransAxisType> allSh = List.of(Rshsh, Tshsh);
    public static final List<ReflTransAxisType> allPSv = List.of(Rpp, Rps, Tpp, Tps, Rsp, Rss, Tsp, Tss);

    public static final List<ReflTransAxisType> allCoeff = List.of(Rpp, Rps, Tpp, Tps, Rsp, Rss, Tsp, Tss, Rshsh, Tshsh);


    public static final List<ReflTransAxisType> allEnergy = List.of(
            RppEnergy, TppEnergy, RpsEnergy, TpsEnergy,
            RspEnergy, TspEnergy, RssEnergy, TssEnergy,
            RshshEnergy, TshshEnergy);

}
