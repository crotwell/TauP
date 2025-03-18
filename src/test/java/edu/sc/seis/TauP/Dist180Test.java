package edu.sc.seis.TauP;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;


public class Dist180Test {

    @Test
    public void constantModelTest() throws TauPException {
        double VP = 5.8;
        VelocityModel vMod = ConstantModelTest.createVelMod(VP, 3.5);
        SlownessModel smod = new SphericalSModel(vMod,0.1,11.0,115.0,2.5*Math.PI/180,0.01,true,
                                   SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        TauModel tMod = new TauModel(smod);
        SeismicPhase PKIKP = SeismicPhaseFactory.createPhase("PKIKP", tMod);
        assertEquals(136.2960951565459, PKIKP.getMinDistanceDeg(), 1e-6);
        assertEquals(180.0, PKIKP.getMaxDistanceDeg(), 1e-6);
        Arrival a = PKIKP.getEarliestArrival(180.0);
        assertEquals(vMod.getRadiusOfEarth()*2/VP, a.getTime(), 0.00001);
    }
    
    @Test
    public void iasp91Test() throws TauPException {
        straightThroughRay("iasp91");
        // ttimes gives 1212.12 seconds, off by ~ 0.03 seconds
    }
    
    @Test
    public void ak135Test() throws TauPException {
        straightThroughRay("ak135");
        // ak135 table gives 1212.53 seconds, off by ~ 0.05 seconds
        // http://rses.anu.edu.au/~brian/AK135tables.pdf
    }
    
    @Test
    public void premTest() throws TauPException {
        straightThroughRay("prem");
    }
    
    /** integrates the velocity model to obtain the travel time for a zero ray parameter ray
     * through the center of the model and back to the surface. As the model is piecewise linear,
     * and the ray parameter is zero and hence the path does not curve, we can obtain an 
     * analytic solution as if the model was a flat stack of layers by using:
     * t = time
     * z = depth in layer
     * d = layer thickness
     * v = velocity, linearly interp a*z+b
     * time = Int_0^d (1/v)
     *      = Int_0^d (1/(a*z+b))
     *      = 1/a * Math.log((a*d+b)/b)
     *      
     * We test to be within 0.005 seconds on this PKIKP ray.
     * 
     * @param modelName
     * @throws TauModelException
     */
    public void straightThroughRay(String modelName) throws TauPException {
        TimeTester time = new TimeTester(modelName);
        time.setSourceDepth(0);
        time.setPhaseNames(Collections.singletonList("PKIKP"));
        List<Arrival> arrivals = time.calcAll(time.getSeismicPhases(), List.of(DistanceRay.ofDegrees(180)));
        assertEquals(integrateVelocity(time.modelArgs.getTauModel().getVelocityModel()), arrivals.get(0).getTime(), 0.006);
    }
    
    /** integrates the velocity model, only works for a zero ray parameter ray. */
    double integrateVelocity(VelocityModel vMod) {
        VelocityLayer[] layers = vMod.getLayers();
        double time = 0;
        for (int i = 0; i < layers.length; i++) {
            double t = layers[i].getThickness();
            if (t == 0) { continue; }
            double dvp = layers[i].getBotPVelocity()-layers[i].getTopPVelocity();
            if (dvp == 0) {
                // const layer
                time += t / layers[i].getTopPVelocity();
            } else {
                // linear vel
                
                double a = dvp / t ;
                double b = layers[i].getTopPVelocity();
                time += 1/a * Math.log((a*t+b)/b);
            }
        }
        return time*2;
    }
}
