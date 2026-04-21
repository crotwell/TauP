package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import net.sf.geographiclib.Geodesic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


import java.util.Arrays;
import java.util.List;

public class PierceTest {


    @Test
    public void PPierce() throws TauModelException, SlownessModelException, NoArrivalException {
        String modelName = "ak135";
        TauModel tMod = TauModelLoader.load(modelName);
        SeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tMod);
        LatLonLocatable evt = new LatLonSimple(35, -50, 100*1000);
        LatLonLocatable sta = new LatLonSimple(30, -81);
        DistanceCalcGeodetic distCalc = new DistanceCalcGeodetic(Geodesic.WGS84);
        DistanceRay dr = DistanceRay.ofEventStation(evt, sta, distCalc);
        List<Arrival> aList = dr.calculate(P_phase);
        assertEquals(3, aList.size());
        Arrival a = aList.get(0);
        assertEquals(true, a.isLatLonable());
        List<TimeDist> pierce = Arrays.asList(a.getPierce());
        TimeDist first = pierce.get(0);
        double[] firstlatlon = a.getLatLonable().calcLatLon(first.getDistDeg(), a.getDistDeg(), first.getDepth());
        assertEquals(evt.asLocation().getLatitude(), firstlatlon[0], 1e-6);
        assertEquals(evt.asLocation().getLongitude(), firstlatlon[1], 1e-6);
        TimeDist last = pierce.get(pierce.size()-1);
        assertEquals(last.getDistDeg(), a.getDistDeg(), 1e-6);
        double[] lastlatlon = a.getLatLonable().calcLatLon(last.getDistDeg(), a.getDistDeg(), last.getDepth());
        assertEquals(sta.asLocation().getLatitude(), lastlatlon[0], 1e-6);
        assertEquals(sta.asLocation().getLongitude(), lastlatlon[1], 1e-6);
    }
}
