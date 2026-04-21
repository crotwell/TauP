package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.dtor;
import static edu.sc.seis.TauP.SphericalCoords.rtod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sf.geographiclib.Geodesic;
import org.junit.jupiter.api.Test;

import java.util.List;


public class SphericalCoordsTest {

    @Test
    public void latlonTest() {
        double stationLat = 37.18f;
        double stationLon = 21.92f;
        double eventLat = -17.84f;
        double eventLon = -178.30f;
        double azimuth = SphericalCoords.azimuth(eventLat,
                                                 eventLon,
                                                 stationLat,
                                                 stationLon);
        double backAzimuth = SphericalCoords.azimuth(stationLat,
                                                     stationLon,
                                                     eventLat,
                                                     eventLon);
        double dist = SphericalCoords.distance(stationLat,
                                               stationLon,
                                               eventLat,
                                               eventLon);
        double lat = SphericalCoords.latFor(eventLat,
                                     eventLon,
                                     dist,
                                     azimuth);
        double lon = SphericalCoords.lonFor(eventLat,
                                     eventLon,
                                     dist,
                                     azimuth);
        assertEquals(stationLat, lat, 0.0001);
        assertEquals(stationLon, lon, 0.0001);
    }
    
    @Test
    public void halfDist() {
        double stationLat = 37.18f;
        double stationLon = 21.92f;
        double eventLat = -17.84f;
        double eventLon = -178.30f;
        double azimuth = SphericalCoords.azimuth(eventLat,
                                                 eventLon,
                                                 stationLat,
                                                 stationLon);
        double backAzimuth = SphericalCoords.azimuth(stationLat,
                                                     stationLon,
                                                     eventLat,
                                                     eventLon);
        double dist = 38.34;
        double lat = SphericalCoords.latFor(eventLat,
                                     eventLon,
                                     dist,
                                     azimuth);
        double lon = SphericalCoords.lonFor(eventLat,
                                     eventLon,
                                     dist,
                                     azimuth);
        assertEquals(12.82, lat, 0.01);
        assertEquals(158.37, lon, 0.01);
        assertEquals(38.34, SphericalCoords.distance(eventLat,
                                               eventLon, 12.82, 158.37), 0.01);
        assertEquals(azimuth, SphericalCoords.azimuth(eventLat,
                                                     eventLon, 12.82, 158.37), 0.01);
    }

    @Test
    public void trim180RadianTest() {
        assertEquals(0, SphericalCoords.getModuloDistRadian(0), 1e-6);
        assertEquals(Math.PI, SphericalCoords.getModuloDistRadian(Math.PI), 1e-6);
        assertEquals(0, SphericalCoords.getModuloDistRadian(2*Math.PI), 1e-6);
        assertEquals(Math.PI/4, SphericalCoords.getModuloDistRadian(-1*Math.PI/4), 1e-6);
        assertEquals(0.001, SphericalCoords.getModuloDistRadian(2*Math.PI-0.001), 1e-6);
        assertEquals(Math.PI-0.0001, SphericalCoords.getModuloDistRadian(3*Math.PI+0.0001), 1e-6);

    }

    @Test
    public void trim180Test() {
        assertEquals(0, SphericalCoords.distanceTrim180(0), 1e-6);
        assertEquals(0.0001, SphericalCoords.distanceTrim180(-0.0001), 1e-6);
        assertEquals(90, SphericalCoords.distanceTrim180(270), 1e-6);
        assertEquals(180, SphericalCoords.distanceTrim180(-179.99999999), 1e-6);
        assertEquals(0, SphericalCoords.distanceTrim180(360), 1e-6);
        assertEquals(0, SphericalCoords.distanceTrim180(720), 1e-6);
        assertEquals(180, SphericalCoords.distanceTrim180(-180), 1e-6);
        assertEquals(20, SphericalCoords.distanceTrim180(340), 1e-6);
        assertEquals(179, SphericalCoords.distanceTrim180(181), 1e-6);
        assertEquals(1, SphericalCoords.distanceTrim180(359), 1e-6);
    }

    @Test
    public void azimuthTest() {
        assertEquals(0, SphericalCoords.azimuth(0,0, 90, 0), 1e-6);
        assertEquals(90, SphericalCoords.azimuth(0,0, 0, 10), 1e-6);
        assertEquals(-90, SphericalCoords.azimuth(0,0, 0, -10), 1e-6);
        assertEquals(180, SphericalCoords.azimuth(0,0, -10, 0), 1e-6);
        double latA = 35;
        double lonA = -81;
        double latB = -24;
        double lonB = -154;
        double wikipedia = rtod*Math.atan2(Math.cos(latB*dtor)*Math.sin((lonB-lonA)*dtor),
                (Math.cos(latA*dtor)*Math.sin(latB*dtor)-Math.sin(latA*dtor)*Math.cos(latB*dtor)*Math.cos((lonB-lonA)*dtor)));
        assertEquals(wikipedia, SphericalCoords.azimuth(latA, lonA, latB, lonB), 1e-6);
    }


    @Test
    public void geocentricAzimuthTest() {
        Geocentric geocentric = new Geocentric(Geodesic.WGS84);
        assertEquals(0, geocentric.azimuth(0, 0, 1, 90, 0, -1), 1e-6);
        assertEquals(90, geocentric.azimuth(0, 0, 1, 0, 10, -1), 1e-6);
        assertEquals(-90, geocentric.azimuth(0, 0, 1, 0, -10, -1), 1e-6);
        assertEquals(180, geocentric.azimuth(0, 0, 1, -10, 0, -1), 1e-6);
    }

    @Test
    public void geocentricDistTest() {
        double depthMeter = 1000*1000;
        double lat = 45;
        double lon = 0;
        Geocentric geocentric = new Geocentric(Geodesic.WGS84);
        List<Double> vAList = geocentric.IntForward(lat, lon, -1*depthMeter, false);
        double[] sphvA = SphericalCoords.xyzFromLatLonRadius(lat, lon, DistAzKarney.averageRadiusMeter(Geodesic.WGS84)-depthMeter);
        assertTrue(Math.abs(vAList.get(0)-sphvA[0]) > 1e-8, "x "+vAList.get(0)+" "+sphvA[0]);
        // y zero for both as lon 0 is x-z plane
        assertEquals(vAList.get(1), sphvA[1] , 1e-8, "y "+vAList.get(1)+" "+sphvA[1]);
        assertTrue(Math.abs(vAList.get(2)-sphvA[2]) > 1e-8, "z "+vAList.get(2)+" "+sphvA[2]);
    }

    @Test
    public void vsDistAz() {

        double lat = 45;
        double lon = 0;
        double latB = -34;
        double lonB = -81;
        Geocentric geocentric = new Geocentric(Geodesic.WGS84);
        DistAz distAz = new DistAz(lat, lon, latB, lonB, Geodesic.WGS84.Flattening());
        assertEquals(distAz.getDelta(), geocentric.angleBetweenDeg(lat, lon, 0, latB, lonB, 0), 1e-3);
    }
}
