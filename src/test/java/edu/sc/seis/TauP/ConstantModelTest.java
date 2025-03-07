package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class ConstantModelTest {

    @BeforeEach
    public void setUp() throws Exception {
        vmod = createVelMod(vp, vs);
        System.err.println();
        PrintWriter pw = new PrintWriter(System.err);
        vmod.writeToND(pw);
        pw.flush();
        System.err.println();
        smod = new SphericalSModel(vmod,
                                   0.1,
                                   11.0,
                                   115.0,
                                   2.5 * Math.PI / 180,
                                   0.01,
                                   true,
                                   SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        tMod = new TauModel(smod);
    }

    public static VelocityModel createVelMod(double vp, double vs) {
        float step = .00000001f;
        List<VelocityLayer> layers = new ArrayList<VelocityLayer>();
        layers.add(new VelocityLayer(0, 0, 30, vp, vp, vs, vs));
        layers.add(new VelocityLayer(0, 30, 2890, vp, vp, vs, vs));
        layers.add(new VelocityLayer(0, 2890, 4000, vp + step, vp, vs, vs));
        layers.add(new VelocityLayer(0, 4000, R, vp + step, vp, vs, vs));
        return new VelocityModel("constant", R, 30, 2890, 4000, 0, R, true, layers);
    }

    public static VelocityModel createVelModLiquidOuterCore(double vp, double vs) {
        float step = .00000001f;
        List<VelocityLayer> layers = new ArrayList<VelocityLayer>();
        layers.add(new VelocityLayer(0, 0, 30, vp, vp, vs, vs));
        layers.add(new VelocityLayer(0, 30, 2890, vp, vp, vs, vs));
        layers.add(new VelocityLayer(0, 2890, 4000, vp + step, vp, 0, 0));
        layers.add(new VelocityLayer(0, 4000, R, vp + step, vp, vs, vs));
        return new VelocityModel("constant", R, 30, 2890, 4000, 0, R, true, layers);
    }

    @Test
    public void testDirectP() {
        doDirectTest(tMod, SeismicPhase.PWAVE);
    }

    @Test
    public void testDirectS() {
        doDirectTest(tMod, SeismicPhase.SWAVE);
    }

/**
 * Error increases with depth, so
 * @throws TauModelException
 */
    @Test
    public void testDepthP() throws Exception {
        for (int depth = 0; depth < 400; depth+=5) {
            for (int deg = 0; deg < 90; deg++) {
                doSeismicPhase(depth, deg, vp, "P", tMod);
            }
        }
    }

    @Test
    public void testCurveFirstDistCheck() throws Exception {
        // all 3 models have same const vel 5.8 uppermost crust (either 15 or 20 km thick)
        // so answer should be the same as a constant 5.8 earth for the horizontal takeoff ray
        // so long as the depth is << 15
        for (double depth = 0; depth < 14; depth+=.1) {
        List<String> modelList = Arrays.asList(new String[] {"iasp91", "ak135", "prem"});
        for (String model : modelList) {
            TauModel tauMod = TauModelLoader.load(model);
            String phase = "P";
            double velocity = tauMod.getVelocityModel().getVelocityLayerClone(0).getTopPVelocity();
            TauModel tModDepth = tauMod.depthCorrect(depth);
            SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phase, tModDepth, depth);
            double dist = pPhase.getDist()[0] * 180 / Math.PI + 0.0001;
            List<Arrival> arrivals = DistanceRay.ofDegrees(dist).calculate(pPhase);
            // find arrival with rp closest to horizontal ray from source
            double minRPdiff = 999999999;
            Arrival a = null;
            double sourceRP = tModDepth.getTauBranch(tModDepth.getSourceBranch(), true).getMaxRayParam();
            for (Arrival arrival : arrivals) {
                if (Math.abs(arrival.getRayParam() - sourceRP) < minRPdiff) {
                    a = arrival;
                    minRPdiff = Math.abs(arrival.getRayParam() - sourceRP);
                }
            }
            assertEquals(lawCosinesLength(R, depth, dist * Math.PI / 180) / velocity,
                         a.getTime(),
                         0.0001,
                         "travel time for " + dist + " depth=" + depth + " at " + velocity);
        }
        }
    }

    /**
     * dist is from source at surface to turning point. Model is constant, so
     * can use sin and velocity to get travel time independently.
     *
     * @param isPWave
     *            true for P false for S
     */
    public static void doDirectTest(TauModel tMod, boolean isPWave) {
        VelocityModel vMod = tMod.getVelocityModel();
        VelocityLayer topLayer = vMod.getVelocityLayer(0);
        double velocity = isPWave ? topLayer.getTopPVelocity() : topLayer.getTopSVelocity();
        for (int i = 0; i < tMod.rayParams.length; i++) {
            float dist = 0;
            float time = 0;
            for (int j = 0; j < tMod.getNumBranches(); j++) {
                dist += tMod.getTauBranch(j, isPWave).getDist(i);
                time += tMod.getTauBranch(j, isPWave).time[i];
            }
            assertEquals(R * Math.sin(dist) / velocity, time, 0.01);
        }
    }

    @Test
    public void testSeismicPhaseDirectP() throws TauModelException {
        float dist = 3;
        double velocity = vp;
        doSeismicPhase(dist, velocity, "P", tMod);
    }

    @Test
    public void testNoInterpSeismicPhaseDirectP() throws TauModelException {
        double velocity = vp;
        boolean isPWave = true;
        for (int i = 0; i < tMod.rayParams.length; i++) {
            float dist = 0;
            for (int j = 0; j < tMod.getNumBranches(); j++) {
                dist += tMod.getTauBranch(j, isPWave).getDist(i);
            }
            doSeismicPhase(2 * dist, velocity, "P", tMod);
        }
    }

    @Test
    public void testTStarDirectP() throws Exception {
        double Qp = vmod.getVelocityLayer(0).getTopQp();//const in model
        double velocity = vp;
        boolean isPWave = true;

        SeismicPhase PPhase = SeismicPhaseFactory.createPhase("P", tMod, tMod.getSourceDepth());
        assertTrue(PPhase.phasesExistsInModel());
        for (int i = 0; i < tMod.rayParams.length; i++) {
            double dist = 0;
            double time = 0;
            List<Arrival> arrivals = DistanceRay.ofDegrees(dist).calculate(PPhase);
            Arrival arrival = arrivals.get(0);
            double tstar = arrival.getTime() / Qp;
            assertEquals(tstar, arrival.calcTStar(), i+" "+tMod.getRayParams().length+" "+arrival);
        }
    }

    public void txestPrint() {
        // System.out.println(smod.toString());
        tMod.print();
    }

    public static void doSeismicPhase(float dist, double velocity, String phase, TauModel tMod) throws TauModelException {
        SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phase, tMod, tMod.sourceDepth);
        List<Arrival> arrivals = DistanceRay.ofDegrees(dist).calculate(pPhase);
        assertEquals( 1, arrivals.size());
        Arrival a = arrivals.get(0);
        assertEquals(2 * R * Math.sin(dist / 2 * Math.PI / 180) / velocity,
                     a.getTime(),
                     0.01);
    }

    public static void doSeismicPhase(double depth, double dist, double velocity, String phase, TauModel tMod)
            throws TauModelException, NoSuchLayerException {
        TauModel tModDepth = tMod.depthCorrect(depth);
        SeismicPhase PPhase = SeismicPhaseFactory.createPhase(phase.toUpperCase(), tModDepth, depth);
        assertTrue(PPhase.phasesExistsInModel());
        DistanceRay distanceRay = DistanceRay.ofDegrees(dist);
        List<Arrival> arrivals = distanceRay.calculate(PPhase);
        SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phase.toLowerCase(), tModDepth, depth);
        if (depth != 0.0) {
            assertTrue(pPhase.phasesExistsInModel());
            arrivals.addAll(distanceRay.calculate(pPhase));
        }
        assertEquals(1, arrivals.size(), "one arrival for "+dist+" depth="+depth+" at "+velocity);
        Arrival a = arrivals.get(0);
        assertEquals(lawCosinesLength(R, depth, dist * Math.PI / 180) / velocity,
                     a.getTime(),
                     0.02, dist+" depth="+depth+" at "+velocity);
    }

    public static double lawCosinesLength(double R, double depth, double theta) {
        double A = (R - depth);
        double B = R;
        return Math.sqrt(A * A + B * B - 2 * A * B * Math.cos(theta));
    }

    protected void tearDown() throws Exception {
        vmod = null;
    }

    VelocityModel vmod;

    SphericalSModel smod;

    TauModel tMod;

    static final double R = 6371;

    double vp = 5.8;

    double vs = 3.5;
}
