package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;


public class ConstantModelTest extends TestCase {

    public void setUp() throws Exception {
        vmod = createVelMod(vp, vs);
        smod = new SphericalSModel(vmod,0.1,11.0,115.0,2.5*Math.PI/180,0.01,true,
                                   SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        tMod = new TauModel(smod);
    }
    
    public static VelocityModel createVelMod(double vp, double vs) {
        float step = .00000001f;
        List<VelocityLayer> layers = new ArrayList<VelocityLayer>();
        layers.add(new VelocityLayer(0, 0, 30, vp, vp, vs, vs));
        layers.add(new VelocityLayer(0, 30, 2890, vp, vp, vs, vs));
        layers.add(new VelocityLayer(0, 2890, 4000, vp+step, vp, vs, vs));
        layers.add(new VelocityLayer(0, 4000, R, vp+step, vp, vs, vs));
        return new VelocityModel("constant", R, 30, 2890, 4000,0, R, true, layers);
    }

    public void testDirectP() {
        doDirectTest(SeismicPhase.PWAVE);
    }
    public void testDirectS() {
        doDirectTest(SeismicPhase.SWAVE);
    }
    
    /** dist is from source at surface to turning point. Model is constant, so
     * can use sin and velocity to get travel time independently.
     * @param isPWave true for P false for S
     */
    void doDirectTest(boolean isPWave) {
        double velocity = isPWave ? vp : vs;
        for(int i = 0; i < tMod.rayParams.length; i++) {
            float dist=0;
            float time = 0;
            for(int j = 0; j < tMod.getNumBranches(); j++) {
                
                dist +=  tMod.getTauBranch(j, isPWave).getDist(i);
                time +=  tMod.getTauBranch(j, isPWave).time[i];
            }
            assertEquals(R*Math.sin(dist)/velocity, time, 0.01);
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
        for(int i = 0; i < tMod.rayParams.length; i++) {
            float dist=0;
            for(int j = 0; j < tMod.getNumBranches(); j++) {
                dist +=  tMod.getTauBranch(j, isPWave).getDist(i);
            }
            doSeismicPhase(2*dist, velocity, "P");
        }
    }
    
    public void txestPrint() {
        //System.out.println(smod.toString());
        tMod.print();
    }
    
    public void doSeismicPhase(float dist, double velocity, String phase) throws TauModelException {
        TauP_Time.DEBUG = true;
        SeismicPhase pPhase = new SeismicPhase(phase, tMod);
        
        List<Arrival> arrivals = pPhase.calcTime(dist);
        assertEquals("one arrival", 1, arrivals.size());
        Arrival a = arrivals.get(0);
        assertEquals("travel time for "+dist, 2*R*Math.sin(dist/2*Math.PI/180)/velocity, a.getTime(), 0.01);
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
