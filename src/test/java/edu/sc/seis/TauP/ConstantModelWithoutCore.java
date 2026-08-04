package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.dtor;
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



    @Test
    public void test120degree() throws TauModelException {
        // model sampling had value very near 120 caused bad dtoddelta due to rounding
        double dist = 120;
        SeismicPhase pPhase = SeismicPhaseFactory.createPhase("P", tMod, tMod.sourceDepth);
        DistanceRay dr = DistanceRay.ofDegrees(dist);
        List<Arrival> arrivals = dr.calculate(pPhase);
        assertEquals(1, arrivals.size());
        Arrival a = arrivals.get(0);
        assertTrue(Math.abs(a.neighborArrival.getDist()-a.getDist())>SimpleContigSeismicPhase.NEIGHBOR_MIN_DIST,
                "n: "+a.neighborArrival.getDist()+"  a: "+a.getDist());
    }

    @Test
    public void dtakeoffddelta() throws TauModelException {

        SeismicPhase pPhase = SeismicPhaseFactory.createPhase("P", tMod, tMod.sourceDepth);

        for (double dist=5; dist < 180; dist+=5) {
            DistanceRay dr = DistanceRay.ofDegrees(dist);
            List<Arrival> arrivals = dr.calculate(pPhase);
            assertEquals(1, arrivals.size());
            Arrival a = arrivals.get(0);
            assertEquals(-0.5, a.getDtakeoffDdeltaRadian(), 0.001,
                    "dist: "+dist+" a: "+a.getDistDeg()+" "+a.getTakeoffAngleDegree()
                            +" n: "+a.neighborArrival.getDistDeg()+" "+a.neighborArrival.getTakeoffAngleDegree());
        }
    }

    public void testGeomSpreadingForPhase(String phase) throws TauModelException {
        double R = tMod.getRadiusOfEarth();
        double r0 = R-tMod.getSourceDepth();
        assertEquals(0, tMod.getSourceDepth());
        SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phase, tMod, tMod.sourceDepth);

        // 0 and 180 are bad distances for geo spreading
        for (double dist=5; dist < 180; dist+=5) {
            DistanceRay dr = DistanceRay.ofDegrees(dist);
            List<Arrival> arrivals = dr.calculate(pPhase);
            assertEquals(1, arrivals.size());
            Arrival a = arrivals.get(0);
            double cosIncident = Math.cos(a.getIncidentAngleRadian());
            assertNotEquals(0.0, cosIncident);
            double G = Math.sqrt(Math.abs(
                    (Math.sin(a.getTakeoffAngleRadian())*a.getDtakeoffDdeltaRadian())
                    / (r0*r0*cosIncident*Math.sin(dr.getRadians()))
            ));
            // double out = Math.abs(Math.sin(getTakeoffAngleRadian())*distTerm*dih_ddelta/
            //                (r0*r0*Math.cos(getIncidentAngleRadian())));
            //G = a.getAmplitudeGeometricSpreadingFactor();
            // constant model, so geometrical spreading is 1/r, ie length of chord
            assertTrue(Double.isFinite(a.getAmplitudeGeometricSpreadingFactor()), "dist: "+dist);
            assertTrue(Double.isFinite(G), G+" G dist: "+dist);
            assertNotEquals(0.0,G, G+" G dist: "+dist);
            double chordLength = 2 * r0 * Math.sin(dist * dtor / 2 );
            double geoSpread = 1.0 / (chordLength); // /cosIncident;
            assertTrue(Double.isFinite(geoSpread), "dist: "+dist+" R: "+R+" cos: "+cosIncident);
            assertNotEquals(0.0, geoSpread, "dist: "+dist+" R: "+R+" cos: "+cosIncident);
            assertEquals(0.0,
                    (geoSpread-G)/geoSpread,
                    0.0132, "dist: "+dist+" geo: "+geoSpread+" G: "+G);
            assertEquals(G, a.getAmplitudeGeometricSpreadingFactor(), 1e-6);
            assertEquals(0.0,
                    (geoSpread-a.getAmplitudeGeometricSpreadingFactor())/geoSpread,
                    0.02, "dist: "+dist+" geo: "+geoSpread+" aGeoSpread: "+a.getAmplitudeGeometricSpreadingFactor());
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
        for (int i = 0; i <= 180.0; i+=5.0) {
            double dist = i;
            double time = 0;
            List<Arrival> arrivals = DistanceRay.ofDegrees(dist).calculate(PPhase);
            Arrival arrival = arrivals.get(0);
            double tstar = arrival.getTime() / Qp;
            assertEquals(tstar, arrival.calcTStar(), 3e-6, i+" "+" "+arrival);
        }
    }

    @Test
    public void testAmpDirectP() throws Exception {
        double R = tMod.getRadiusOfEarth();
        double r0 = R-tMod.getSourceDepth();
        assertEquals(R, r0); // surface source
        VelocityLayer top = tMod.getVelocityModel().getVelocityLayer(0);
        double sourceVel = top.getTopPVelocity();
        double sourceDensity = tMod.getVelocityModel().getVelocityLayer(0).getTopDensity();
        double Qp = vmod.getVelocityLayer(0).getTopQp();//const in model
        SeismicPhase PPhase = SeismicPhaseFactory.createPhase("P", tMod, tMod.getSourceDepth());
        assertTrue(PPhase.phasesExistsInModel());
        for (int i = 1; i < 180.0; i+=5.0) {
            double dist = i;
            double chordLength = 2 * r0 * Math.sin(dist * dtor / 2 );
            double time = chordLength/sourceVel; // constant velocity
            DistanceRay dr = DistanceRay.ofDegrees(dist);
            dr.setSeismicSource(new SeismicSource());
            List<Arrival> arrivals = dr.calculate(PPhase);
            assertEquals(1, arrivals.size());
            Arrival arrival = arrivals.get(0);
            assertEquals(time, arrival.getTime(), 1e-5, "time: "+i+" "+" "+arrival);

            double tstar = arrival.getTime() / Qp;
            assertEquals(0.0,Math.abs(tstar- arrival.calcTStar())/tstar, 0.015, "tstar: "+i+" "+tstar+"=?"+arrival.calcTStar()+" "+arrival);

            double amp = 1;
            ReflTransFreeSurface rtFree = ReflTransFreeSurface.createReflTransFreeSurface(top.getTopPVelocity(), top.getTopSVelocity(), top.getTopDensity());
            Complex[] freeSurfRF = rtFree.getFreeSurfaceReceiverFunP(arrival.getRayParam() / R);
            double freeFactor = Complex.abs(Complex.sqrt(freeSurfRF[0].times(freeSurfRF[0]).plus(freeSurfRF[1].times(freeSurfRF[1]))));
            amp *= freeFactor;

            double radiationTerm = 4*Math.PI*sourceDensity*sourceVel*sourceVel*sourceVel*1e12;
            assertEquals(radiationTerm, arrival.calcRadiationTerm(), 1e-8);
            amp *= 1.0/radiationTerm;
            double freq = 1.0; // 1 Hz
            double attenuation = Math.pow(Math.E, -1 * Math.PI * freq * tstar);
            amp *= attenuation;
            assertTrue(Double.isFinite(amp));
            double geoSpread = 1.0 / (2 * R * Math.sin(dist / 2 * Math.PI / 180));
            assertTrue(Double.isFinite(geoSpread), geoSpread+" "+R+" "+dist);
            assertEquals(0.0, (geoSpread-arrival.getAmplitudeGeometricSpreadingFactor())/geoSpread, 2e-2,dist+" "+PPhase.getName());
            amp *= geoSpread;
            assertTrue(Double.isFinite(amp));
            amp *= dr.getMoment();
            assertTrue(Double.isFinite(amp));
            amp *= 1.0 / 1e3;
            assertEquals(1.0, arrival.getEnergyFluxFactorReflTransPSV(), 1e-8);
            double calcAmp = arrival.getAmplitudeFactorPSV(dr.getMoment(), 1.0, 1);
            assertEquals(0.0, Math.abs((amp- calcAmp)/amp), 0.05, dist+" "+PPhase.getName());
        }
    }

    VelocityModel vmod;

    SphericalSModel smod;

    TauModel tMod;
}
