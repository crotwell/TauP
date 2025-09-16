package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import org.junit.jupiter.api.Test;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RadiationPatternTest {

    @Test
    public void testRadPat() {
        double strike = 0 * DtoR;
        double dip = 90 * DtoR;
        double rake = 0 * DtoR;
        double azimuth = 45 * DtoR;
        double takeoff = 90 * DtoR;  // horizontal
        double[] radTerms = SeismicSourceArgs.calcRadiationPatRadian(strike, dip, rake, azimuth, takeoff);
        assertEquals(1, radTerms[0]);
        assertEquals(0, radTerms[1], 1e-9, "Sv");
        assertEquals(0, radTerms[2], 1e-9, "Sh");

        takeoff = 90 * DtoR;
        azimuth = 0 * DtoR;
        radTerms = SeismicSourceArgs.calcRadiationPatRadian(strike, dip, rake, azimuth, takeoff);
        assertEquals(0, radTerms[0], 1e-9);
        assertEquals(1, Math.sqrt(radTerms[1]*radTerms[1]+radTerms[2]*radTerms[2]), 1e-9, "S+Sh");

        // down
        takeoff = 0;
        radTerms = SeismicSourceArgs.calcRadiationPatRadian(strike, dip, rake, azimuth, takeoff);
        assertEquals(0, radTerms[0]);
        assertEquals(0, radTerms[1], 1e-9, "Sv");
        assertEquals(0, radTerms[2], 1e-9, "Sh");


    }

    @Test
    public void testPSymmetry() {
        double strike = 0 * DtoR;
        double dip = 90 * DtoR;
        double rake = 0 * DtoR;
        double azimuth = 45 * DtoR;
        double takeoff = 45 * DtoR;
        for (int i = 0; i < 90; i++) {
            azimuth=i*DtoR;
            double[] radA = SeismicSourceArgs.calcRadiationPatRadian(strike, dip, rake, azimuth, takeoff);
            double[] radB = SeismicSourceArgs.calcRadiationPatRadian(strike, dip, rake, azimuth+Math.PI/2, takeoff);
            double[] radC = SeismicSourceArgs.calcRadiationPatRadian(strike, dip, rake, azimuth+Math.PI, takeoff);
            double[] radD = SeismicSourceArgs.calcRadiationPatRadian(strike, dip, rake, azimuth+3*Math.PI/2, takeoff);
            assertEquals(radA[0], -1*radB[0], 1e-9);
            assertEquals(radA[0], radC[0], 1e-9);
            assertEquals(radA[0], -1*radD[0], 1e-9);
            //S symmetry
            assertEquals(radA[1], -1*radB[1], 1e-9);
            assertEquals(radA[1], radC[1], 1e-9);
            assertEquals(radA[1], -1*radD[1], 1e-9);
            //Sh symmetry
            assertEquals(radA[2], -1*radB[2], 1e-9);
            assertEquals(radA[2], radC[2], 1e-9);
            assertEquals(radA[2], -1*radD[2], 1e-9);
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
