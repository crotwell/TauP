package edu.sc.seis.TauP;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
