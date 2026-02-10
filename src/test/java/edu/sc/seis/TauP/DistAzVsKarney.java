package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.GeoMath;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DistAzVsKarney {

    @Test
    public void dist() {
        //2024-08-30T09:32:19 5.6 mww 52.61/-33.27 10.00 km to IU_KBS 78.92/11.94
        double eLat = 52.19;
        double eLon = -33.27;
        double sLat = 78.92;
        double sLon = 11.94;

        DistAz distaz = new DistAz(eLat, eLon, sLat, sLon);
        Geodesic geodesic = Geodesic.WGS84;
        GeodesicLine azGLine = geodesic.InverseLine(eLat, eLon,
                sLat, sLon);
        GeodesicLine bazGLine = geodesic.InverseLine(sLat, sLon,
                eLat, eLon);
        double avgRaduis = geodesic.EquatorialRadius()* (3- geodesic.Flattening()) / 3 / 1000; // m to km
        assertEquals(6371, avgRaduis, 1.0);
        double distKm = azGLine.Distance()/1000;
        double valDeg = distKm/DistAz.kmPerDeg(avgRaduis);
        DistanceKmRay drKm = DistanceRay.ofKilometers(distKm);
        DistanceAngleRay drDeg = DistanceRay.ofDegrees(valDeg);
        assertEquals(valDeg, drDeg.getDegrees(), 0.00001);
        assertEquals(distKm, drKm.getKilometers(), 0.00001);
        assertEquals(distKm, drDeg.getKilometers(), 0.00001);
        assertEquals(distaz.getDelta(), valDeg, 1.0); // ellip vs spherical can be large
    }

    @Test
    public void latlonableGeodetic() {
        Geodesic geod = Geodesic.WGS84;
        double avgRaduis = RayCalculateable.averageRadiusKm(geod);
        double eLat = 52.19;
        double eLon = -33.27;
        LatLonLocatable evt = new LatLonSimple(eLat, eLon);
        double sLat = 78.92;
        double sLon = 11.94;
        LatLonLocatable sta = new LatLonSimple(sLat, sLon);
        DistAz distAz = new DistAz(evt, sta);

        DistanceAngleRay evtStaDR = DistanceRay.ofGeodeticEventStation(evt, sta, Geodesic.WGS84);
        assertTrue(evtStaDR.hasAzimuth());
        double azimuth = evtStaDR.getAzimuth();
        assertEquals(GeoMath.AngNormalize(distAz.getAz()), azimuth, 0.1, "spherical is close");
        double backazimuth = evtStaDR.getBackAzimuth();
        assertEquals(GeoMath.AngNormalize(distAz.getBaz()), backazimuth, 0.1, "spherical is close");
        double evtStaDegree = evtStaDR.getDegrees();
        assertEquals(distAz.getDelta(), evtStaDegree, 0.1, "spherical is close");

        DistanceRay evtAzDR = DistanceRay.ofDegrees(evtStaDegree);
        evtAzDR.withEventAzimuth(evt, azimuth, geod);
        assertTrue(evtAzDR.isLatLonable());
        double[] evtAzToSta = evtAzDR.getLatLonable().calcLatLon(evtStaDegree, evtStaDegree);
        assertEquals(sLat, evtAzToSta[0], 0.0001);
        assertEquals(sLon, evtAzToSta[1], 0.0001);

        DistanceRay staBazDR = DistanceRay.ofDegrees(evtStaDegree);
        staBazDR.withStationBackAzimuth(sta, backazimuth, geod);
        assertTrue(staBazDR.isLatLonable());
        double[] staBazToEvt = staBazDR.getLatLonable().calcLatLon(0, evtStaDegree);
        assertEquals(eLat, staBazToEvt[0], 0.0001);
        assertEquals(eLon, staBazToEvt[1], 0.0001);
    }


    public void printDist() {
        double eLon = 0;
        double sLat;
        double sLon;
        System.err.println("i   sta lat,lon  distaz   lambert   (diff)  karney arc  (diff)  lam-karney");
        List<List<Double>> fibPoints = fibonacci_sphere(2000);
        List<Double> maxDKarney = List.of( 0.0, 0.0, 0.0, 0.0, 0.0);
        List<Double> maxDSpherical = List.of( 0.0, 0.0, 0.0, 0.0, 0.0);

        double avgRaduis = Geodesic.WGS84.EquatorialRadius()* (3- Geodesic.WGS84.Flattening()) / 3;
        double m_to_deg = 1/avgRaduis*SphericalCoords.RtoD;
        System.err.println("Average Radius: "+avgRaduis+" for eq: "+Geodesic.WGS84.EquatorialRadius()+" flat: "+Geodesic.WGS84.Flattening());

        for (int eLat = 0; eLat <= 90; eLat+=20) {
            for (List<Double> point : fibPoints) {
                sLat = point.get(0);
                sLon = point.get(1);

                double spherical = SphericalCoords.distance(eLat, eLon, sLat, sLon);
                DistAz daz = new DistAz(eLat, eLon, sLat, sLon);
                DistAzLambert dazlam = new DistAzLambert(eLat, eLon, sLat, sLon);
                GeodesicLine geodesicLine = Geodesic.WGS84.InverseLine(eLat, eLon, sLat, sLon);

                double karneyAvgR = geodesicLine.Distance()*m_to_deg;

                if (false) System.err.println(" "+sLat+","+sLon+"  "+daz.getDelta()+" "+dazlam.getDelta()
                        +" "+(daz.getDelta()-dazlam.getDelta())
                        +"  "+karneyAvgR+" "+(daz.getDelta()- geodesicLine.Arc())+"  "+(dazlam.getDelta()- geodesicLine.Arc()));
                double dKarney = (daz.getDelta()- karneyAvgR);
                if (Math.abs(maxDKarney.get(0)) < Math.abs(dKarney)) {
                    maxDKarney = List.of( dKarney, (double) eLat, eLon, sLat, sLon, daz.getDelta(), karneyAvgR, spherical);
                }
                double dSpherical = (daz.getDelta()- spherical);
                if (Math.abs(maxDSpherical.get(0)) < Math.abs(dSpherical)) {
                    maxDSpherical = List.of( dSpherical, (double) eLat, eLon, sLat, sLon, daz.getDelta(), karneyAvgR, spherical);
                }

            }
        }
        System.err.println();
        System.err.println("max delta karney: "+maxDKarney.get(0)+" for "+maxDKarney.get(1)+","+maxDKarney.get(2)+" to "+maxDKarney.get(3)+","+maxDKarney.get(4));
        System.err.println("daz: "+maxDKarney.get(5)+"  k: "+maxDKarney.get(6)+" sph: "+maxDKarney.get(7));
        System.err.println();
        System.err.println("max delta spherical: "+maxDSpherical.get(0)+" for "+maxDSpherical.get(1)+","+maxDSpherical.get(2)+" to "+maxDSpherical.get(3)+","+maxDSpherical.get(4));
        System.err.println("daz: "+maxDSpherical.get(5)+"  k: "+maxDSpherical.get(6)+" sph: "+maxDSpherical.get(7));

        GeodesicLine polarGeodesicLine = Geodesic.WGS84.InverseLine(0, 0, 90, 0);
        GeodesicLine equatorGeodesicLine = Geodesic.WGS84.InverseLine(0, 0, 0, 90);
        System.err.println("90 deg, polar: "+polarGeodesicLine.Arc()+" equitorial: "+equatorGeodesicLine.Arc());
        System.err.println("6371, polar: "+(polarGeodesicLine.Distance()/1000/6371*SphericalCoords.RtoD)
                +" equitorial: "+(equatorGeodesicLine.Distance()/1000/6371*SphericalCoords.RtoD));

        System.err.println();
        System.err.println("Polar "+polarGeodesicLine.Arc()+" deg, "+polarGeodesicLine.Distance()+" m");
        System.err.println("Equitorial "+equatorGeodesicLine.Arc()+" deg, "+equatorGeodesicLine.Distance()+" m");

        System.err.println();
        System.err.println("Direct");
        GeodesicData polarPos = polarGeodesicLine.Position(polarGeodesicLine.Distance());
        GeodesicData equiPos = equatorGeodesicLine.Position(equatorGeodesicLine.Distance());
        System.err.println("Polar "+polarPos.lat2+","+polarPos.lon2);
        System.err.println("Equitorial "+equiPos.lat2+","+equiPos.lon2);

        System.err.println();
        System.err.println("Arc");
        GeodesicData polarArcPos = polarGeodesicLine.ArcPosition(polarGeodesicLine.Arc());
        GeodesicData equiArcPos = equatorGeodesicLine.ArcPosition(equatorGeodesicLine.Arc());
        System.err.println("Polar "+polarArcPos.lat2+","+polarArcPos.lon2);
        System.err.println("Equitorial "+equiArcPos.lat2+","+equiArcPos.lon2);

        System.err.println();
        System.err.println("Arc 90");
        GeodesicData polar90ArcPos = polarGeodesicLine.ArcPosition(90);
        GeodesicData equi90ArcPos = equatorGeodesicLine.ArcPosition(90);
        System.err.println("Polar "+polar90ArcPos.lat2+","+polar90ArcPos.lon2);
        System.err.println("Equitorial "+equi90ArcPos.lat2+","+equi90ArcPos.lon2);


        assertTrue(false);
    }


    public List<List<Double>> fibonacci_sphere(int number_points) {
        // https://stackoverflow.com/questions/9600801/evenly-distributing-n-points-on-a-sphere
        List<List<Double>> points = new ArrayList<>();

        double phi = Math.PI * (Math.sqrt(5.) - 1.);  //golden angle in radians

        for (int i = 0; i < number_points; i++) {
            double y = 1 - (i / (number_points - 1.0)) *2;  //y goes from 1 to - 1
            double radius = Math.sqrt(1 - y * y);  //radius at y

            double theta = phi * i;  //golden angle increment

            double x = Math.cos(theta) * radius;
            double z = Math.sin(theta) * radius;
            double longitude = (Math.atan2(y, x))*SphericalCoords.RtoD;
            double xy_hypot = Math.hypot(x, y);
            double latitude = Math.atan(z / xy_hypot)*SphericalCoords.RtoD;

            points.add(List.of(latitude, longitude, x, y, z));
        }

        return points;
    }
}
