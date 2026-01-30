package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


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
}
