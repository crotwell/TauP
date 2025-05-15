package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.PhaseInteraction.REFLECT_UNDERSIDE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class AddToBranchTest {
    @Test
    public void printTMod() {
        for (int i = 0; i < tMod.getNumBranches(); i++) {
            System.out.println(tMod.getTauBranch(i,true));
            System.out.println(tMod.getTauBranch(i,false));
        }
        System.out.println("surface: "+p_surface_rp+" "+s_surface_rp);
        System.out.println("crust: "+p_crust_rp+" "+s_crust_rp);
        System.out.println("mantle: "+p_mantle_rp+" "+s_mantle_rp);
        System.out.println("cmb: "+p_cmb_rp+" "+s_cmb_rp);
        System.out.println("ocore: "+p_ocore_rp+"  -1");
        assertEquals(0,0);
    }

    /*
    surface: 1098.448275862069 1820.2857142857142
    crust: 1093.2758620689656 1811.7142857142858
    mantle: 812.948717948718 1152.909090909091
    cmb: 446.28205128205127 632.9090909090909
    ocore: 355.204081632653  -1
     */

    @Test
    public void turn_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("P");
        seisFactory.addToBranch(0,PWAVE,PWAVE,PhaseInteraction.TURN, "P");
        assertEquals(p_surface_rp, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(p_crust_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void turn_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("S");
        seisFactory.addToBranch(0,SWAVE,SWAVE,PhaseInteraction.TURN, "S");
        assertEquals(s_surface_rp, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(s_crust_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void reflecttop_P_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Pvmp");
        seisFactory.addToBranch(0, PWAVE, PWAVE, PhaseInteraction.REFLECT_TOPSIDE, "P");
        assertEquals(p_crust_rp, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void reflecttop_P_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Pvms");
        seisFactory.addToBranch(0, PWAVE, SWAVE, PhaseInteraction.REFLECT_TOPSIDE, "P");
        double maxRP = Math.min(p_crust_rp, s_crust_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void reflecttop_S_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Svmp");
        seisFactory.addToBranch(0, SWAVE, PWAVE, PhaseInteraction.REFLECT_TOPSIDE, "S");
        double maxRP = Math.min(p_crust_rp, s_crust_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void reflecttop_S_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Svms");
        seisFactory.addToBranch(0,SWAVE,SWAVE,PhaseInteraction.REFLECT_TOPSIDE, "S");
        assertEquals(s_crust_rp, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void reflecttopCritical_P_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("PVcp");
        seisFactory.addToBranch(0,PWAVE,PWAVE,PhaseInteraction.REFLECT_TOPSIDE_CRITICAL, "P");
        assertEquals(p_crust_rp, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(p_mantle_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void reflecttopCritical_S_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("SVcs");
        seisFactory.addToBranch(0,SWAVE,SWAVE,PhaseInteraction.REFLECT_TOPSIDE_CRITICAL, "S");
        assertEquals(s_crust_rp, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(s_mantle_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transdown_P_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("PmP");
        seisFactory.addToBranch(0,PWAVE,PWAVE,PhaseInteraction.TRANSDOWN, "P");
        double maxRP = Math.min(p_crust_rp, p_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transdown_P_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("PmS");
        seisFactory.addToBranch(0,PWAVE,SWAVE,PhaseInteraction.TRANSDOWN, "P");
        double maxRP = Math.min(p_crust_rp, s_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transdown_S_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("SmP");
        seisFactory.addToBranch(0,SWAVE,PWAVE,PhaseInteraction.TRANSDOWN, "S");
        double maxRP = Math.min(s_crust_rp, p_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transdown_S_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("SmS");
        seisFactory.addToBranch(0,SWAVE,SWAVE,PhaseInteraction.TRANSDOWN, "S");
        double maxRP = Math.min(s_crust_rp, s_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transup_P_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Pmp");
        seisFactory.addToBranch(1,PWAVE,PWAVE,PhaseInteraction.TURN, "P");
        seisFactory.addToBranch(1,PWAVE,PWAVE,PhaseInteraction.TRANSUP, "p");
        double maxRP = Math.min(p_crust_rp, p_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(p_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transup_P_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Pms");
        seisFactory.addToBranch(1,PWAVE,PWAVE,PhaseInteraction.TURN, "P");
        seisFactory.addToBranch(1,PWAVE,SWAVE,PhaseInteraction.TRANSUP, "P");
        double maxRP = Math.min(p_mantle_rp, s_crust_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(p_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transup_S_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Smp");
        seisFactory.addToBranch(1,SWAVE,SWAVE,PhaseInteraction.TURN, "S");
        seisFactory.addToBranch(1,SWAVE,PWAVE,PhaseInteraction.TRANSUP, "S");
        double maxRP = Math.min(s_mantle_rp, p_crust_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(s_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transup_S_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("Sms");
        seisFactory.addToBranch(1,SWAVE,SWAVE,PhaseInteraction.TURN, "S");
        seisFactory.addToBranch(1,SWAVE,SWAVE,PhaseInteraction.TRANSUP, "S");
        double maxRP = Math.min(s_crust_rp, s_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(s_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void refl_under_P_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("P^mP");
        seisFactory.addToBranch(1,PWAVE,PWAVE,PhaseInteraction.TURN, "P");
        seisFactory.addToBranch(1,PWAVE,PWAVE, REFLECT_UNDERSIDE, "P");
        double maxRP = Math.min(p_crust_rp, p_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(p_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void refl_under_P_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("P^ms");
        seisFactory.addToBranch(1,PWAVE,PWAVE,PhaseInteraction.TURN, "P");
        seisFactory.addToBranch(1,PWAVE,SWAVE,REFLECT_UNDERSIDE, "P");
        double maxRP = Math.min(p_mantle_rp, s_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(p_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void refl_under_S_P() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("S^mp");
        seisFactory.addToBranch(1,SWAVE,SWAVE,PhaseInteraction.TURN, "S");
        seisFactory.addToBranch(1,SWAVE,PWAVE,REFLECT_UNDERSIDE, "S");
        double maxRP = Math.min(s_mantle_rp, p_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(s_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void refl_under_S_S() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("S^ms");
        seisFactory.addToBranch(1,SWAVE,SWAVE,PhaseInteraction.TURN, "S");
        seisFactory.addToBranch(1,SWAVE,SWAVE,REFLECT_UNDERSIDE, "S");
        double maxRP = Math.min(s_crust_rp, s_mantle_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(s_cmb_rp, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void transdown_S_K_core() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("SKS");
        seisFactory.addToBranch(1,SWAVE,PWAVE,PhaseInteraction.TRANSDOWN, "S");
        double maxRP = Math.min(s_cmb_rp, p_ocore_rp);
        assertEquals(maxRP, seisFactory.endSegment().maxRayParam, 0.0001);
        assertEquals(0, seisFactory.endSegment().minRayParam, 0.0001);
    }

    @Test
    public void legsArePWaveTest() throws TauModelException {
        ProtoSeismicPhase seisFactory = makeSPhFactory("PKP");
        ArrayList<String> legsPKP = LegPuller.legPuller(seisFactory.getName());
        boolean[] legsArePWave = SeismicPhaseFactory.legsArePWave(legsPKP);
        for (boolean b : legsArePWave) {
            assertEquals(true, b);
        }
        seisFactory = makeSPhFactory("SKS");
        ArrayList<String> legs = LegPuller.legPuller(seisFactory.getName());
        legsArePWave = SeismicPhaseFactory.legsArePWave(legs);
        // legs includes END as leg
        assertEquals(4, legsArePWave.length);
        assertEquals(false, legsArePWave[0]);
        assertEquals(true, legsArePWave[1]);
        assertEquals(false, legsArePWave[2]);
        assertEquals(false, legsArePWave[2]);
    }

    public static ProtoSeismicPhase makeSPhFactory(String name) throws TauModelException {
        boolean isPWave = true;
        if (name.startsWith("S") || name.startsWith("s") || name.startsWith("J") || name.startsWith("j")) {
            isPWave = false;
        }
        ProtoSeismicPhase proto = ProtoSeismicPhase.startEmpty(name, tMod, 0);
        List<String> legs = LegPuller.legPuller(name);
        //seisFactory.legs = legs;
        return proto;
    }

    @BeforeAll
    public static void createTMod() throws SlownessModelException, TauModelException, NoSuchLayerException {
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

        List<VelocityLayer> layers = new ArrayList<VelocityLayer>();
        layers.add(new VelocityLayer(0, 0, 30, vp, vp, vs, vs));
        layers.add(new VelocityLayer(1, 30, 2890, vp+step, vp+step, vs+step, vs+step));
        layers.add(new VelocityLayer(2, 2890, 4000, vp + 2*step, vp+2*step, 0, 0));
        layers.add(new VelocityLayer(3, 4000, R, vp + 3*step, vp+3*step, vs+3*step, vs+3*step));
        return new VelocityModel("verysimple_"+step, R, 30, 2890, 4000, 0, R, true, layers);
    }

    static TauModel tMod;

    static VelocityModel vmod;

    static SphericalSModel smod;

    static boolean PWAVE = true;
    static boolean SWAVE = false;

    static final double R = 6371;

    static double vp = 5.8;

    static double vs = 3.5;

    /**
     * Velocity step between crust and mantle and between mantle and outercore.
     */
    static float step = 2.00f;

    static double p_surface_rp = (R)/vp;
    static double s_surface_rp = (R)/vs;
    static double p_crust_rp = (R-30)/vp;
    static double s_crust_rp = (R-30)/vs;
    static double p_mantle_rp = (R-30)/(vp+step);
    static double s_mantle_rp = (R-30)/(vs+step);
    static double p_cmb_rp = (R-2890)/(vp+step);
    static double s_cmb_rp = (R-2890)/(vs+step);
    static double p_ocore_rp = (R-2890)/(vp+2*step);  // outer core should be lvz for P???

}
