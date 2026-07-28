package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ConstantModelWithoutCore {

    public ConstantModelWithoutCore() throws Exception {
        vmod = VelocityModelTest.loadTestVelMod("constant.tvel");
        assertNotNull(vmod);
        smod = new SphericalSModel(vmod,
                                   0.1,
                                   11.0,
                                   115.0,
                                   2.5 * Math.PI / 180,
                                   0.01,
                                   true,
                                   SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        assertNotNull(smod);
        tMod = new TauModel(smod);
        assertNotNull(tMod);
    }

    @Test
    public void testCMBatCenter() {
        assertEquals( 0.0, tMod.getMohoDepth(), 0.000001);
        assertEquals( tMod.getRadiusOfEarth(), tMod.getCmbDepth(), 0.000001);
    }

    @Test
    public void testDirectP() {
        ConstantModelTest.doDirectTest(tMod, SeismicPhase.PWAVE);
    }

    @Test
    public void testDirectS() {
        ConstantModelTest.doDirectTest(tMod, SeismicPhase.SWAVE);
    }
    
    @Test
    public void testNoCorePhase() throws TauModelException {
        String[] badPhaseList = new String[] {"PKP", "PKIKP", "PIP", "PcP", "PKiKP", "SKS" };
        for (String phaseName : badPhaseList) {
                SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phaseName, tMod);
                assertFalse( pPhase.phasesExistsInModel(), phaseName+" should not exist in model");
                List<Arrival> arrivals = DistanceRay.ofDegrees(130).calculate(pPhase);
                assertEquals( 0, arrivals.size());
        }
    }
    
/**
 * Error increases with depth, so 
 * @throws TauModelException
 */
    @Test
    public void testDepthP() throws Exception {
        String phase = "P";
        TauModel tModDepth = tMod.depthCorrect(0);
        SeismicPhase PPhase = SeismicPhaseFactory.createPhase(phase.toUpperCase(), tModDepth, 0, 0,true);

        double vp = tMod.getVelocityModel().getVelocityLayer(0).getTopPVelocity();
        for (int depth = 0; depth < 400; depth += 5) {
            for (int deg = 0; deg < 90; deg++) {
                ConstantModelTest.doSeismicPhase(depth, deg, vp, phase, tMod);
            }
        }
    }


    @Test
    public void testSeismicPhaseDirectP() throws TauModelException {
        float dist = 3;
        double velocity = tMod.getVelocityModel().getVelocityLayer(0).getTopPVelocity();
        ConstantModelTest.doSeismicPhase(dist, velocity, "P", tMod);
    }

    @Test
    public void testNoInterpSeismicPhaseDirectP() throws TauModelException {
        assertNotNull(tMod);
        assertNotNull(tMod.getVelocityModel());
        assertNotNull(tMod.getVelocityModel().getVelocityLayer(0));
        double velocity = tMod.getVelocityModel().getVelocityLayer(0).getTopPVelocity();
        boolean isPWave = true;
        for (int i = 0; i < tMod.rayParams.length; i++) {
            float dist = 0;
            for (int j = 0; j < tMod.getNumBranches(); j++) {
                dist += tMod.getTauBranch(j, isPWave).getDist(i);
            }
            ConstantModelTest.doSeismicPhase(2 * dist, velocity, "P", tMod);
        }
    }

    @Test
    public void testGeomSpreadingP() throws TauModelException {
        String phase = "P";
        testGeomSpreadingForPhase(phase);
    }

    @Test
    public void testGeomSpreadingS() throws TauModelException {
        String phase = "S";
        testGeomSpreadingForPhase(phase);
    }

    public void testGeomSpreadingForPhase(String phase) throws TauModelException {
        double R = tMod.getRadiusOfEarth();
        assertEquals(0, tMod.getSourceDepth());
        SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phase, tMod, tMod.sourceDepth);
        for (double dist=10; dist <= 180; dist+=10) {
            List<Arrival> arrivals = DistanceRay.ofDegrees(dist).calculate(pPhase);
            assertEquals(1, arrivals.size());
            Arrival a = arrivals.get(0);
            // constant model, so geometrical spreading is 1/r, ie length of chord
            assertFalse(Double.isNaN(a.getAmplitudeGeometricSpreadingFactor()), "dist: "+dist);
            assertEquals(1.0 / (2 * R * Math.sin(dist / 2 * Math.PI / 180)),
                    a.getAmplitudeGeometricSpreadingFactor(),
                    0.01, "dist: "+dist);
        }
    }

    @Test
    public void testTStarDirectP() throws Exception {
        double Qp = vmod.getVelocityLayer(0).getTopQp();//const in model
        double velocity = tMod.getVelocityModel().getVelocityLayer(0).getTopPVelocity();
        boolean isPWave = true;

        SeismicPhase PPhase = SeismicPhaseFactory.createPhase("P", tMod, tMod.getSourceDepth());
        assertTrue(PPhase.phasesExistsInModel());
        assertEquals(0, PPhase.getMinDistanceDeg(), 1e-6);
        assertEquals(180, PPhase.getMaxDistanceDeg(), 1e-6);
        for (int i = 0; i <= 180.0; i+=30.0) {
            double dist = i;
            double time = 0;
            List<Arrival> arrivals = DistanceRay.ofDegrees(dist).calculate(PPhase);
            Arrival arrival = arrivals.get(0);
            double tstar = arrival.getTime() / Qp;
            assertEquals(tstar, arrival.calcTStar(), 1e-6, i+" "+" "+arrival);
        }
    }

    @Test
    public void testAmpDirectP() throws Exception {
        double R = tMod.getRadiusOfEarth();
        VelocityLayer top = tMod.getVelocityModel().getVelocityLayer(0);
        double sourceVel = top.getTopPVelocity();
        double sourceDensity = tMod.getVelocityModel().getVelocityLayer(0).getTopDensity();
        double Qp = vmod.getVelocityLayer(0).getTopQp();//const in model
        SeismicPhase PPhase = SeismicPhaseFactory.createPhase("P", tMod, tMod.getSourceDepth());
        assertTrue(PPhase.phasesExistsInModel());
        for (int i = 30; i <= 180.0; i+=30.0) {
            double dist = i;
            double time = 0;
            DistanceRay dr = DistanceRay.ofDegrees(dist);
            dr.setSeismicSource(new SeismicSource());
            List<Arrival> arrivals = dr.calculate(PPhase);
            assertEquals(1, arrivals.size());
            Arrival arrival = arrivals.get(0);
            double tstar = arrival.getTime() / Qp;
            assertEquals(tstar, arrival.calcTStar(), 1e-6, i+" "+" "+arrival);

            double amp = 1;
            ReflTransFreeSurface rtFree = ReflTransFreeSurface.createReflTransFreeSurface(top.getTopPVelocity(), top.getTopSVelocity(), top.getTopDensity());
            Complex[] freeSurfRF = rtFree.getFreeSurfaceReceiverFunP(arrival.getRayParam() / R);
            double freeFactor = Complex.abs(Complex.sqrt(freeSurfRF[0].times(freeSurfRF[0]).plus(freeSurfRF[1].times(freeSurfRF[1]))));
            amp *= freeFactor;

            double radiationTerm = 4*Math.PI*sourceDensity*sourceVel*sourceVel*sourceVel*1e12;
            assertEquals(radiationTerm, arrival.calcRadiationTerm(), 1e-6);
            amp *= 1.0/radiationTerm;
            double freq = 1.0; // 1 Hz
            amp *= Math.pow(Math.E, -1 * Math.PI * freq * tstar);
            assertTrue(Double.isFinite(amp));
            double geoSpread = 1.0 / (2 * R * Math.sin(dist / 2 * Math.PI / 180));
            assertTrue(Double.isFinite(geoSpread), geoSpread+" "+R+" "+dist);
            amp *= geoSpread;
            assertTrue(Double.isFinite(amp));
            amp *= dr.getMoment();
            assertTrue(Double.isFinite(amp));
            amp *= 1.0 / 1e3;
            assertEquals(1.0, arrival.getEnergyFluxFactorReflTransPSV(), 1e-6);
            assertEquals(amp, arrival.getAmplitudeFactorPSV(dr.getMoment(), 1.0, 1), 1e-6);
        }
    }

    VelocityModel vmod;

    SphericalSModel smod;

    TauModel tMod;
}
