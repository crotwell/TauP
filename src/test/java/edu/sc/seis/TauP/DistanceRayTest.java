package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicLine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistanceRayTest {

    @Test
    public void calcRadiansInRangeTest() {
        DistanceRay pi4dr = DistanceRay.ofRadians(Math.PI/4);
        List<Double> pi4distList = pi4dr.calcRadiansInRange(0, Math.PI/2, 6371, true);
        assertEquals(1, pi4distList.size());
        assertEquals(Math.PI/4, pi4distList.get(0));

        DistanceRay dr = DistanceRay.ofDegrees(10);
        List<Double> distList = dr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(4, distList.size());
        assertEquals(10*Math.PI/180, distList.get(0));
        assertEquals(350*Math.PI/180, distList.get(1), 1e-6);
        assertEquals(2*Math.PI+10*Math.PI/180, distList.get(2), 1e-6);
        assertEquals(2*Math.PI+350*Math.PI/180, distList.get(3), 1e-6);
        ExactDistanceRay edr = DistanceRay.ofExactDegrees(210);
        List<Double> edistList = edr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(1, edistList.size());

        FixedHemisphereDistanceRay fdr = DistanceRay.ofFixedHemisphereDegrees(210);
        List<Double> fdistList = fdr.calcRadiansInRange(0, 4*Math.PI, 6371, true);
        assertEquals(2, fdistList.size());

    }

    @Test
    public void testGeodesic() {
        double staLat = 10;
        double staLon = 0;
        double evtLat = 0;
        double evtLon = 0;
        GeodesicLine bazGLine = Geodesic.WGS84.InverseLine(staLat, staLon,
                evtLat, evtLon);
        double baz = bazGLine.Azimuth();
        assertEquals(180.0, baz, "baz 10,0 to 0,0");

        GeodesicLine azGLine = Geodesic.WGS84.InverseLine(
                evtLat, evtLon, staLat, staLon);
        double az = azGLine.Azimuth();
        assertEquals(0.0, az, "az 10,0 to 0,0");

    }

    @Test
    public void testCalcAzBaz() {
        DistanceCalcSpherical distCalcSph = DistanceCalcSpherical.EARTH_SPHERE;
        DistanceCalcGeodetic distCalcGeodetic = new DistanceCalcGeodetic(Geodesic.WGS84);
        Location staLoc = new Location(10, 0);
        Location evtLoc = new Location(0, 0);
        DistanceRay dr = DistanceRay.ofEventStation(evtLoc, staLoc, distCalcSph);
        assertEquals(0, dr.getNormalizedAzimuth(), 0.01);
        assertEquals(180, dr.getNormalizedBackAzimuth(), 0.01);

        DistanceRay gdr = DistanceRay.ofEventStation(evtLoc, staLoc, distCalcGeodetic);
        assertEquals(dr.getNormalizedAzimuth(), gdr.getNormalizedAzimuth(), 0.1);
        assertEquals(dr.getNormalizedBackAzimuth(), gdr.getNormalizedBackAzimuth(), 0.1);


        Location staLocE = new Location(10, 10);
        DistanceRay drE = DistanceRay.ofEventStation(evtLoc, staLocE, distCalcSph);
        assertEquals(45, drE.getNormalizedAzimuth(), 1);
        assertEquals(-135, drE.getNormalizedBackAzimuth(), 1);
        DistanceRay gdrE = DistanceRay.ofEventStation(evtLoc, staLocE, distCalcGeodetic);
        assertEquals(drE.getNormalizedAzimuth(), gdrE.getNormalizedAzimuth(), 0.5, "az");
        assertEquals(drE.getNormalizedBackAzimuth(), gdrE.getNormalizedBackAzimuth(), 0.5, "baz");

        Location evtLocW = new Location(0, -10);
        DistanceRay drW = DistanceRay.ofEventStation(evtLocW, staLoc, distCalcSph);
        assertEquals(45, drW.getNormalizedAzimuth(), 1);
        assertEquals(-135, drW.getNormalizedBackAzimuth(), 1);
        DistanceRay gdrW = DistanceRay.ofEventStation(evtLocW, staLoc, distCalcGeodetic);
        assertEquals(drW.getNormalizedAzimuth(), gdrW.getNormalizedAzimuth(), 0.5);
        assertEquals(drW.getNormalizedBackAzimuth(), gdrW.getNormalizedBackAzimuth(), 0.5);
    }

    @Test
    public void surfaceWaveDist() throws TauModelException {
        double deg = 270;
        ExactDistanceRay dr = DistanceRay.ofExactDegrees(deg);
        SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase("2kmps", TauModelLoader.load("prem"));
        List<Arrival> arrList = dr.calculate(phase);
        assertEquals(1, arrList.size());
        assertEquals(deg, arrList.get(0).getDistDeg());
    }

    @Test
    public void poleToPoleTest() {
        DistanceCalcSpherical distCalcSph = DistanceCalcSpherical.EARTH_SPHERE;
        DistanceCalcGeocentric distCalcGeocen = new DistanceCalcGeocentric(Geodesic.WGS84);
        DistanceCalcGeodetic distCalcGeod = new DistanceCalcGeodetic(Geodesic.WGS84);
        LatLonSimple np = new LatLonSimple(90, 0);
        LatLonSimple equator = new LatLonSimple(0, 0);
        DistanceRay sphDR = DistanceRay.ofEventStation(np, equator, distCalcSph);
        DistanceRay gcDR = DistanceRay.ofEventStation(np, equator, distCalcGeocen);
        DistanceRay gdDR = DistanceRay.ofEventStation(np, equator, distCalcGeod);

        Geocentric geocentric = new Geocentric(Geodesic.WGS84);
        List<Double> vAList = geocentric.IntForward(np.asLocation().getLatitude(), np.asLocation().getLongitude(), 0, false);
        assertEquals(0, vAList.get(0), 1000);
        assertEquals(0, vAList.get(1), 1000);
        assertEquals(6356*1000, vAList.get(2), 1000);
        List<Double> vBList = geocentric.IntForward(equator.asLocation().getLatitude(), equator.asLocation().getLongitude(), 0, false);
        assertEquals(6378*1000, vBList.get(0), 1000);
        assertEquals(0, vBList.get(1), 1000);
        assertEquals(0, vBList.get(2), 1000);

        double geocDist = geocentric.angleBetweenDeg(np.asLocation().getLatitude(), np.asLocation().getLongitude(), -1*0,
                equator.asLocation().getLatitude(), equator.asLocation().getLongitude(), -1* 0);
        assertEquals(90.0, geocDist, 1.0);


        assertEquals(90.0, sphDR.getDegrees(), 1.0);
        assertEquals(90.0, gcDR.getDegrees(), 1.0);
        assertEquals(90.0, gdDR.getDegrees(), 1.0);
    }
}
