package edu.sc.seis.TauP;


import static org.junit.jupiter.api.Assertions.assertEquals;

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
    
    /**
     *      
     * We test to be within 0.005 seconds on this PKIKP ray.
     * 
     * @param modelName
     * @throws TauModelException
     */
    public void straightThroughRay(String modelName) throws TauPException {
        TimeTester tool = new TimeTester(modelName);
        tool.setSourceDepth(0);
        VelocityModel vMod = tool.modelArgs.getTauModel().getVelocityModel();

        tool.setPhaseNames(Collections.singletonList("PKIKP"));
        List<Arrival> arrivals = tool.calcAll(tool.getSeismicPhases(), List.of(DistanceRay.ofDegrees(180)));
        double[] layerTimes = integrateVelocity(tool.modelArgs.getTauModel().getVelocityModel());
        double time_PIKIP = 0;
        for (int i = 0; i < layerTimes.length; i++) {
            time_PIKIP += 2 * layerTimes[i];
        }
        assertEquals(time_PIKIP, arrivals.get(0).getTime(), 0.0056, "PKIKP delta: " + (time_PIKIP - arrivals.get(0).getTime()));

        // PcP
        int cmb = vMod.layerNumberBelow(vMod.getCmbDepth());
        double time_PcP = 0;
        for (int i = 0; i < cmb; i++) {
            time_PcP += 2 * layerTimes[i];
        }
        tool.setPhaseNames(Collections.singletonList("PcP"));
        List<Arrival> PcP_arrivals = tool.calcAll(tool.getSeismicPhases(), List.of(DistanceRay.ofDegrees(0)));
        Arrival PcP = PcP_arrivals.get(0);
        assertEquals(time_PcP, PcP.getTime(), 0.002);
    }

    
    /**
     * integrates the velocity model to obtain the travel time for a zero ray parameter ray
     *      * through the model  for each layer. As the model is piecewise linear,
     *      * and the ray parameter is zero and hence the path does not curve, we can obtain an
     *      * analytic solution as if the model was a flat stack of layers by using:
     *      * t = time
     *      * z = depth in layer
     *      * d = layer thickness
     *      * v = velocity, linearly interp a*z+b
     *      * time = Int_0^d (1/v)
     *      *      = Int_0^d (1/(a*z+b))
     *      *      = 1/a * Math.log((a*d+b)/b)
     *
     * integrates the velocity model, only works for a zero ray parameter ray.
     * @return one way travel time through each layer
     * */
    public static double[] integrateVelocity(VelocityModel vMod) {
        VelocityLayer[] layers = vMod.getLayers();
        double[] time = new double[layers.length];
        System.err.println("Model: "+vMod.getModelName());
        for (int i = 0; i < layers.length; i++) {
            double t = layers[i].getThickness();
            if (t == 0) { continue; }
            double dvp = layers[i].getBotPVelocity()-layers[i].getTopPVelocity();
            if (dvp == 0) {
                // const layer
                time[i] = t / layers[i].getTopPVelocity();
            } else {
                // linear vel
                
                double a = dvp / t ;
                double b = layers[i].getTopPVelocity();
                time[i] = 1/a * Math.log((a*t+b)/b);
            }
            System.err.println(i+" "+time[i]+" d: "+layers[i].getBotDepth());
        }
        return time;
    }
}
