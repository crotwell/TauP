package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RadiationPatternTest {

    @Test
    public void testRadPat() {
        double strike = 0;
        double dip = 90;
        double rake = 0;
        double azimuth = 45;
        double takeoff = 90;  // horizontal
        FaultPlane faultPlane = new FaultPlane(strike, dip, rake);
        RadiationAmplitude radTerms = faultPlane.calcRadiationPatDegree(azimuth, takeoff);
        assertEquals(1, radTerms.getRadialAmplitude());
        assertEquals(0, radTerms.getPhiAmplitude(), 1e-9, "Sv");
        assertEquals(0, radTerms.getThetaAmplitude(), 1e-9, "Sh");

        takeoff = 90;
        azimuth = 0;
        radTerms = faultPlane.calcRadiationPatDegree(azimuth, takeoff);
        assertEquals(0, radTerms.getRadialAmplitude(), 1e-9);
        assertEquals(1, Math.sqrt(radTerms.getPhiAmplitude()*radTerms.getPhiAmplitude()
                +radTerms.getThetaAmplitude()*radTerms.getThetaAmplitude()), 1e-9, "S+Sh");

        // down
        takeoff = 0;
        radTerms = faultPlane.calcRadiationPatDegree(azimuth, takeoff);
        assertEquals(0, radTerms.getRadialAmplitude());
        assertEquals(0, radTerms.getPhiAmplitude(), 1e-9, "Sv");
        assertEquals(0, radTerms.getThetaAmplitude(), 1e-9, "Sh");


    }

    @Test
    public void testPSymmetry() {
        double strike = 0;
        double dip = 90;
        double rake = 0;
        FaultPlane faultPlane = new FaultPlane(strike, dip, rake);
        double azimuth = 45;
        double takeoff = 45;
        for (int i = 0; i < 90; i++) {
            azimuth=i;
            RadiationAmplitude radA = faultPlane.calcRadiationPatDegree(azimuth, takeoff);
            RadiationAmplitude radB = faultPlane.calcRadiationPatDegree(azimuth+90, takeoff);
            RadiationAmplitude radC = faultPlane.calcRadiationPatDegree(azimuth+180, takeoff);
            RadiationAmplitude radD = faultPlane.calcRadiationPatDegree(azimuth+270, takeoff);
            assertEquals(radA.getRadialAmplitude(), -1*radB.getRadialAmplitude(), 1e-9);
            assertEquals(radA.getRadialAmplitude(), radC.getRadialAmplitude(), 1e-9);
            assertEquals(radA.getRadialAmplitude(), -1*radD.getRadialAmplitude(), 1e-9);
            //S symmetry
            assertEquals(radA.getPhiAmplitude(), -1*radB.getPhiAmplitude(), 1e-9);
            assertEquals(radA.getPhiAmplitude(), radC.getPhiAmplitude(), 1e-9);
            assertEquals(radA.getPhiAmplitude(), -1*radD.getPhiAmplitude(), 1e-9);
            //Sh symmetry
            assertEquals(radA.getThetaAmplitude(), -1*radB.getThetaAmplitude(), 1e-9);
            assertEquals(radA.getThetaAmplitude(), radC.getThetaAmplitude(), 1e-9);
            assertEquals(radA.getThetaAmplitude(), -1*radD.getThetaAmplitude(), 1e-9);
        }
    }

    @Test
    public void testSphericalCoordinate() {
        double takeoff = 90;
        assertEquals(Math.PI/2, SphericalCoordinate.takeoffDegreeToPhiRadian(takeoff), 1e-9);
        SphericalCoordinate coord = SphericalCoordinate.fromAzTakeoffDegree(0, takeoff);
        assertEquals(Math.PI/2, coord.getPhi(), 1e-9);
        assertEquals(takeoff, coord.getTakeoffAngleDegree(), 1e-9);
        double sterR = coord.stereoR();
        assertEquals(1, sterR, 1e-9, "phi="+coord.getPhi());
    }

}
