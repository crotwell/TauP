package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OceanModelTest {

    @Test
    void ak135favg_ModelLoadTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        String modelName = "ak135favg.nd";
        String modelType = "nd";
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);

        for (NamedVelocityDiscon nd : vMod.namedDiscon) {
            if (NamedVelocityDiscon.isIceBed(nd.name)) {
                assertEquals(0, nd.depth);
            }
            if (NamedVelocityDiscon.isSeabed(nd.name)) {
                assertEquals(3, nd.depth);
            }
        }
        assertEquals(10, vMod.getMohoDepth());
        assertEquals(2891.50, vMod.getCmbDepth());
        assertEquals(5153.50, vMod.getIocbDepth());
        TauP_Create taupCreate = new TauP_Create();
        TauModel tMod = taupCreate.createTauModel(vMod);
        assertEquals(10, tMod.getNumBranches());
        assertEquals(808, tMod.getRayParams().length);
        assertEquals(2, tMod.getSlownessModel().fluidLayerDepths.size());
        String phase = "P";
        double dist = 30;
        SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phase, tMod, tMod.sourceDepth);
        List<Arrival> arrivals = pPhase.calcTime(dist);
        assertEquals(1, arrivals.size());
        Arrival a = arrivals.get(0);
        assertEquals(371.95,
                a.getTime(),
                0.01);
    }

    /**
     * Create a velocity model identical to iasp91, but with a 3 km ocean on top. Compare travel times for phases
     * shifting source depth by ocean and putting receiver at bottom of ocean.
     *
     * @throws TauModelException
     * @throws VelocityModelException
     * @throws SlownessModelException
     */
    @Test
    public void iasp91_ocean() throws TauModelException, VelocityModelException, SlownessModelException {

        String modelName = "iasp91";
        TauModel tMod = TauModelLoader.load(modelName);
        ArrayList<VelocityLayer> vlayers = new ArrayList<VelocityLayer>();
        int laynum = 0;
        VelocityLayer ocean = new VelocityLayer(
                laynum++,
                0,
                3,
                1.4,
                1.4,
                .9,
                .9
        );
        vlayers.add(ocean);
        VelocityLayer prev = ocean;
        for (VelocityLayer vl : tMod.getVelocityModel().getLayers()) {
            VelocityLayer layer = new VelocityLayer(
                    laynum++,
                    prev.getBotDepth(),
                    prev.getBotDepth() + vl.getThickness(),
                    vl.getTopPVelocity(),
                    vl.getBotPVelocity(),
                    vl.getTopSVelocity(),
                    vl.getBotSVelocity());
            vlayers.add(layer);
            prev = layer;
        }
        VelocityModel oceanVMod = new VelocityModel(
                "ocean",
                prev.getBotDepth(),
                tMod.getMohoDepth(),
                tMod.getCmbDepth(),
                tMod.getIocbDepth(),
                0,
                prev.getBotDepth(),
                true,
                vlayers);
        TauP_Create create = new TauP_Create();
        TauModel oceanTMod = create.createTauModel(oceanVMod);
        TauP_Time ocean_time = new TauP_Time(oceanTMod);
        TauP_Time crust_time = new TauP_Time(modelName);
        String[] phaseList = new String[]{"P", "S", "PKP", "PKIKP"};
        double[] depths = new double[]{0, 5, 10, 45, 100, 300};
        for (double depth : depths) {
            TauModel ocean_tmod_depth = oceanTMod.depthCorrect(depth + ocean.getBotDepth());
            TauModel crust_tmod_depth = tMod.depthCorrect(depth);
            for (String phasename : phaseList) {
                SeismicPhase oceanPh = SeismicPhaseFactory.createPhase(phasename, ocean_tmod_depth, depth + ocean.getBotDepth(), ocean.getBotDepth());
                SeismicPhase crustPh = SeismicPhaseFactory.createPhase(phasename, crust_tmod_depth, depth, 0);
                for (float deg = 0; deg < 180; deg += 5) {
                    List<Arrival> ocean_arr = oceanPh.calcTime(deg);
                    List<Arrival> crust_arr = crustPh.calcTime(deg);
                    assertEquals(crust_arr.size(), ocean_arr.size());
                    for (int i = 0; i < crust_arr.size(); i++) {
                        Arrival ocean_a = ocean_arr.get(i);
                        Arrival crust_a = crust_arr.get(i);
                        assertEquals(crust_a.getTime(), ocean_a.getTime(), 0.001);
                    }
                    ;
                }
            }
        }
    }

    @Test
    public void europaModelTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        String europa_modelname = "EuropaLike.nd";
        VelocityModel europaVMod = VelocityModelTest.loadTestVelMod(europa_modelname);
        for (NamedVelocityDiscon nd : europaVMod.namedDiscon) {
            if (NamedVelocityDiscon.isIceBed(nd.name)) {
                assertEquals(20, nd.depth);
            }
            if (NamedVelocityDiscon.isSeabed(nd.name)) {
                assertEquals(100, nd.depth);
            }
        }
        assertEquals(105, europaVMod.getMohoDepth());
        assertEquals(900, europaVMod.getCmbDepth());
        assertEquals(europaVMod.getRadiusOfEarth(), europaVMod.getIocbDepth());
        TauP_Create taupCreate = new TauP_Create();
        TauModel europaTMod = taupCreate.createTauModel(europaVMod);

        String[] phaseList = new String[]{"P", "PKP", "PKIKP"};
        double[] depths = new double[]{0, 5, 10, 45, 100, 300};
        for (double depth : depths) {
            TauModel europa_tmod_depth = europaTMod.depthCorrect(depth);
            for (String phasename : phaseList) {
                SeismicPhase seisPh = SeismicPhaseFactory.createPhase(phasename, europa_tmod_depth, depth, 0);
                for (float deg = 0; deg < 180; deg += 5) {
                    List<Arrival> arrivalList = seisPh.calcTime(deg);
                    // this is not a good test, other than that no errors occur
                    assertNotEquals(-1, arrivalList.size(), phasename);
                }
            }
        }
    }

    @Test
    void ioModelLoadTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        VelocityModel ioVMod = VelocityModelTest.loadTestVelMod("IoLike.nd");
        assertEquals(10, ioVMod.getMohoDepth());
        assertEquals(900, ioVMod.getCmbDepth());
        assertEquals(ioVMod.getRadiusOfEarth(), ioVMod.getIocbDepth());
        TauP_Create taupCreate = new TauP_Create();
        TauModel tMod = taupCreate.createTauModel(ioVMod);

    }

    @Test
    void onlyCoreModelLoadTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        VelocityModel ioVMod = VelocityModelTest.loadTestVelMod("allCore.nd");
        assertEquals(0, ioVMod.getCmbDepth());
        assertEquals(5154.9, ioVMod.getIocbDepth());
        TauP_Create taupCreate = new TauP_Create();
        TauModel tMod = taupCreate.createTauModel(ioVMod);
        String phasename = "KIK";
        SeismicPhase seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        float deg = 30;
        List<Arrival> arrivalList = seisPh.calcTime(deg);
        // this is not a good test, other than that no errors occur
        assertNotEquals(-1, arrivalList.size(), phasename);
        phasename = "PKIKP";
        seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        arrivalList = seisPh.calcTime(deg);
        assertEquals(0, arrivalList.size(), phasename);
    }

    @Test
    void onlyInnerCoreModelLoadTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        VelocityModel ioVMod = VelocityModelTest.loadTestVelMod("allInnerCore.nd");
        assertEquals(0, ioVMod.getMohoDepth());
        assertEquals(0, ioVMod.getCmbDepth());
        assertEquals(0, ioVMod.getIocbDepth());
        TauP_Create taupCreate = new TauP_Create();
        TauModel tMod = taupCreate.createTauModel(ioVMod);
        assertEquals(0, tMod.getMohoDepth());
        assertEquals(0, tMod.getCmbDepth());
        assertEquals(0, tMod.getIocbDepth());
        String phasename = "I";
        SeismicPhase seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        float deg = 30;
        List<Arrival> arrivalList = seisPh.calcTime(deg);
        // this is not a good test, other than that no errors occur
        assertNotEquals(-1, arrivalList.size(), phasename);
        phasename = "PKIKP";
        seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        arrivalList = seisPh.calcTime(deg);
        assertEquals(0, arrivalList.size(), phasename);
        phasename = "KIK";
        seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        arrivalList = seisPh.calcTime(deg);
        assertEquals(0, arrivalList.size(), phasename);
    }
}
