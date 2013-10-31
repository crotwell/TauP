package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

public class ConstantModelTest extends TestCase {

    public void setUp() throws Exception {
        vmod = createVelMod(vp, vs);
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

    @Test
    public void testDirectP() {
        doDirectTest(SeismicPhase.PWAVE);
    }

    @Test
    public void testDirectS() {
        doDirectTest(SeismicPhase.SWAVE);
    }
    
/**
 * Error increases with depth, so 
 * @throws TauModelException
 */
    @Test
    public void testDepthP() throws TauModelException {
        for (int depth = 0; depth < 400; depth++) {
            for (int deg = 0; deg < 90; deg++) {
                doSeismicPhase(depth, deg, vp, "P");
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
            SeismicPhase pPhase = new SeismicPhase(phase, tModDepth);
            double dist = pPhase.getDist()[0] * 180 / Math.PI + 0.0001;
            List<Arrival> arrivals = pPhase.calcTime(dist);
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
            assertEquals("travel time for " + dist + " depth=" + depth + " at " + velocity,
                         lawCosinesLength(R, depth, dist * Math.PI / 180) / velocity,
                         a.getTime(),
                         0.0001);
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
    void doDirectTest(boolean isPWave) {
        double velocity = isPWave ? vp : vs;
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

    public void testSeismicPhaseDirectP() throws TauModelException {
        float dist = 3;
        double velocity = vp;
        doSeismicPhase(dist, velocity, "P");
    }

    public void testNoInterpSeismicPhaseDirectP() throws TauModelException {
        double velocity = vp;
        boolean isPWave = true;
        for (int i = 0; i < tMod.rayParams.length; i++) {
            float dist = 0;
            for (int j = 0; j < tMod.getNumBranches(); j++) {
                dist += tMod.getTauBranch(j, isPWave).getDist(i);
            }
            doSeismicPhase(2 * dist, velocity, "P");
        }
    }

    public void txestPrint() {
        // System.out.println(smod.toString());
        tMod.print();
    }

    public void doSeismicPhase(float dist, double velocity, String phase) throws TauModelException {
        SeismicPhase pPhase = new SeismicPhase(phase, tMod);
        List<Arrival> arrivals = pPhase.calcTime(dist);
        assertEquals("one arrival", 1, arrivals.size());
        Arrival a = arrivals.get(0);
        assertEquals("travel time for " + dist,
                     2 * R * Math.sin(dist / 2 * Math.PI / 180) / velocity,
                     a.getTime(),
                     0.01);
    }

    public void doSeismicPhase(double depth, double dist, double velocity, String phase) throws TauModelException {
        doSeismicPhase(depth, dist, velocity, phase, tMod);
    }

    public static void doSeismicPhase(double depth, double dist, double velocity, String phase, TauModel tMod)
            throws TauModelException {
        TauModel tModDepth = tMod.depthCorrect(depth);
        SeismicPhase PPhase = new SeismicPhase(phase.toUpperCase(), tModDepth);
        SeismicPhase pPhase = new SeismicPhase(phase.toLowerCase(), tModDepth);
        List<Arrival> arrivals = PPhase.calcTime(dist);
        arrivals.addAll(pPhase.calcTime(dist));
        // assertEquals("one arrival for "+dist+" depth="+depth+" at "+velocity,
        // 1, arrivals.size());
        Arrival a = arrivals.get(0);
        assertEquals("travel time for " + dist + " depth=" + depth + " at " + velocity,
                     lawCosinesLength(R, depth, dist * Math.PI / 180) / velocity,
                     a.getTime(),
                     0.02);
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
