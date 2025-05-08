package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.PhaseArgs;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
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
        assertEquals(18, vMod.getMohoDepth());
        assertEquals(2891.50, vMod.getCmbDepth());
        assertEquals(5153.50, vMod.getIocbDepth());
        TauModel tMod = TauModelLoader.createTauModel(vMod);
        assertEquals(10, tMod.getNumBranches());
        assertEquals(979, tMod.getRayParams().length);
        assertEquals(2, tMod.getSlownessModel().fluidLayerDepths.size());
        String phase = "P";
        double dist = 30;
        SeismicPhase pPhase = SeismicPhaseFactory.createPhase(phase, tMod, tMod.sourceDepth);
        List<Arrival> arrivals = DistanceRay.ofDegrees(dist).calculate(pPhase);
        assertEquals(1, arrivals.size());
        Arrival a = arrivals.get(0);
        assertEquals(371.95,
                a.getTime(),
                0.01);

    }

    @Test
    public void ttallOcean() throws TauModelException, SlownessModelException {
        String modelName = "ak135favg.nd";
        TauModel tMod = TauModelLoader.load(modelName);
        List<String> phaseNameList = PhaseArgs.extractPhaseNames("ttall");
        List<SeismicPhase> phaseList = new ArrayList<>();
        for (String pn : phaseNameList) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(pn, tMod, tMod.sourceDepth);
            phaseList.add(sp);
        }
        SeismicSourceArgs sourceArgs = new SeismicSourceArgs();
        double dist = 5;
        for (SeismicPhase sp : phaseList) {
            DistanceRay dr = DistanceRay.ofDegrees(dist);
            dr.setSourceArgs(sourceArgs);
            List<Arrival> arrivals = dr.calculate(sp);
            for (Arrival aa : arrivals) {
                assertNotNull(aa);
                assertNotNull(aa.getAmplitudeFactorPSV(), sp.getName());

            }
        }
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
    public void iasp91_ocean() throws TauModelException, VelocityModelException, SlownessModelException, IOException {

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
        TauModel oceanTMod = TauModelLoader.createTauModel(oceanVMod);
        String[] phaseList = new String[]{"P", "S", "PKP", "PKIKP"};
        double[] depths = new double[]{0, 5, 10, 45, 100, 300};
        for (double depth : depths) {
            TauModel ocean_tmod_depth = oceanTMod.depthCorrect(depth + ocean.getBotDepth());
            TauModel crust_tmod_depth = tMod.depthCorrect(depth);
            for (String phasename : phaseList) {
                SeismicPhase oceanPh = SeismicPhaseFactory.createPhase(phasename, ocean_tmod_depth, depth + ocean.getBotDepth(), ocean.getBotDepth());
                SeismicPhase crustPh = SeismicPhaseFactory.createPhase(phasename, crust_tmod_depth, depth, 0);
                for (float deg = 0; deg < 180; deg += 5) {
                    DistanceRay distanceRay = DistanceRay.ofDegrees(deg);
                    List<Arrival> ocean_arr = distanceRay.calculate(oceanPh);
                    List<Arrival> crust_arr = distanceRay.calculate(crustPh);
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
        TauModel europaTMod = TauModelLoader.createTauModel(europaVMod);

        String[] phaseList = new String[]{"P", "PKP", "PKIKP",
                "Pv" + PhaseSymbols.NAMED_DISCON_START + "ocean-crust" + PhaseSymbols.NAMED_DISCON_END + "s"};
        double[] depths = new double[]{0, 5, 10, 45, 100, 300};
        for (double depth : depths) {
            TauModel europa_tmod_depth = europaTMod.depthCorrect(depth);
            for (String phasename : phaseList) {
                SeismicPhase seisPh = SeismicPhaseFactory.createPhase(phasename, europa_tmod_depth, depth, 0);
                for (float deg = 0; deg < 180; deg += 5) {
                    List<Arrival> arrivalList = DistanceRay.ofDegrees(deg).calculate(seisPh);
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
        TauModel tMod = TauModelLoader.createTauModel(ioVMod);

    }

    @Test
    void onlyCoreModelLoadTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        VelocityModel ioVMod = VelocityModelTest.loadTestVelMod("allCore.nd");
        assertEquals(0, ioVMod.getCmbDepth());
        assertEquals(5154.9, ioVMod.getIocbDepth());
        TauModel tMod = TauModelLoader.createTauModel(ioVMod);
        String phasename = "KIK";
        SeismicPhase seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        float deg = 30;
        DistanceRay distanceRay = DistanceRay.ofDegrees(deg);
        List<Arrival> arrivalList = distanceRay.calculate(seisPh);
        // this is not a good test, other than that no errors occur
        assertNotEquals(-1, arrivalList.size(), phasename);
        phasename = "PKIKP";
        seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        arrivalList = distanceRay.calculate(seisPh);
        assertEquals(0, arrivalList.size(), phasename);
    }

    @Test
    void onlyInnerCoreModelLoadTest() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
        VelocityModel ioVMod = VelocityModelTest.loadTestVelMod("allInnerCore.nd");
        assertEquals(0, ioVMod.getMohoDepth());
        assertEquals(0, ioVMod.getCmbDepth());
        assertEquals(0, ioVMod.getIocbDepth());
        TauModel tMod = TauModelLoader.createTauModel(ioVMod);
        assertEquals(0, tMod.getMohoDepth());
        assertEquals(0, tMod.getCmbDepth());
        assertEquals(0, tMod.getIocbDepth());
        String phasename = "I";
        SeismicPhase seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        float deg = 30;
        DistanceRay distanceRay = DistanceRay.ofDegrees(deg);
        List<Arrival> arrivalList = distanceRay.calculate(seisPh);
        // this is not a good test, other than that no errors occur
        assertNotEquals(-1, arrivalList.size(), phasename);
        phasename = "PKIKP";
        seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        arrivalList = distanceRay.calculate(seisPh);
        assertEquals(0, arrivalList.size(), phasename);
        phasename = "KIK";
        seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        arrivalList = distanceRay.calculate(seisPh);
        assertEquals(0, arrivalList.size(), phasename);
    }

    @Test
    public void marsLiquidLowerMantleTest() throws VelocityModelException, IOException, SlownessModelException, TauModelException {
        VelocityModel marsVMod = VelocityModelTest.loadTestVelMod("MarsLiquidLowerMantle.nd");
        assertEquals(0.0, marsVMod.getVelocityLayer(marsVMod.layerNumberAbove(1560)).getTopSVelocity());
        TauModel tMod = TauModelLoader.createTauModel(marsVMod);
        assertEquals(tMod.getCmbDepth(), 1679.894f, 0.001);
        String phasename = "S";
        SeismicPhase seisPh = SeismicPhaseFactory.createPhase(phasename, tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel());
        seisPh = SeismicPhaseFactory.createPhase("SS", tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel());
        seisPh = SeismicPhaseFactory.createPhase("SKS", tMod, 0, 0);
        assertFalse(seisPh.phasesExistsInModel());
        seisPh = SeismicPhaseFactory.createPhase("S1554PKP1554S", tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel());
        seisPh = SeismicPhaseFactory.createPhase("Pdiff^1554Pdiff", tMod, 0, 0);
        assertFalse(seisPh.phasesExistsInModel());

        String depthOfDiscon = "1554";
        String disconLiqSil = PhaseSymbols.NAMED_DISCON_START+marsCustomDiscon+PhaseSymbols.NAMED_DISCON_END;

        String depthDisconPhase = "S1554Pcp";
        seisPh = SeismicPhaseFactory.createPhase(depthDisconPhase, tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel());
        String namedDisconPhase = depthDisconPhase.replaceAll(depthOfDiscon, disconLiqSil);
        seisPh = SeismicPhaseFactory.createPhase(namedDisconPhase, tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel());

        assertTrue(LegPuller.isBoundary(disconLiqSil));
        assertEquals(LegPuller.closestDisconBranchToDepth(tMod,depthOfDiscon),
                LegPuller.closestDisconBranchToDepth(tMod, disconLiqSil));
        assertEquals(LegPuller.legAsDepthBoundary(tMod, depthOfDiscon),
                LegPuller.legAsDepthBoundary(tMod, disconLiqSil));

        seisPh = SeismicPhaseFactory.createPhase(namedDisconPhase, tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel(), namedDisconPhase);

        String twoDisconPhase = "S1554PKP1554S";
        String twoNamedDisconPhase = twoDisconPhase.replaceAll(depthOfDiscon, disconLiqSil);
        seisPh = SeismicPhaseFactory.createPhase(twoDisconPhase, tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel());
        seisPh = SeismicPhaseFactory.createPhase(twoNamedDisconPhase, tMod, 0, 0);
        assertTrue(seisPh.phasesExistsInModel(), twoNamedDisconPhase);

        // maybe doesn't exist as CMB is in shadow zone from liquid silicate layer
        String undersidePhase = "Pdiff^1554Pdiff";
        String undersideNamedDisconPhase = undersidePhase.replaceAll(depthOfDiscon, disconLiqSil);
        seisPh = SeismicPhaseFactory.createPhase(undersidePhase, tMod, 0, 0);
        assertFalse(seisPh.phasesExistsInModel(), undersidePhase);
        seisPh = SeismicPhaseFactory.createPhase(undersideNamedDisconPhase, tMod, 0, 0);
        assertFalse(seisPh.phasesExistsInModel(), undersideNamedDisconPhase);

    }


    @Test
    public void marsLiquidLowerMantleDiff() throws VelocityModelException, IOException, SlownessModelException, TauModelException {
        VelocityModel marsVMod = VelocityModelTest.loadTestVelMod("MarsLiquidLowerMantle.nd");
        assertEquals(0.0, marsVMod.getVelocityLayer(marsVMod.layerNumberAbove(1560)).getTopSVelocity());
        TauModel tMod = TauModelLoader.createTauModel(marsVMod);
        String Pdiff = "Pdiff";
        assertTrue(PhaseSymbols.isDiffracted(Pdiff, 0), "isDiffracted "+Pdiff+" re: "+LegPuller.namedHeadDiffRE) ;
        String liqsilDiffName = "P" + PhaseSymbols.NAMED_DISCON_START + marsCustomDiscon + PhaseSymbols.NAMED_DISCON_END + "diff";
        assertTrue(PhaseSymbols.isDiffracted(liqsilDiffName, 0), "isDiffracted "+liqsilDiffName);
        SeismicPhase liqsilDiff = SeismicPhaseFactory.createPhase(liqsilDiffName, tMod, 0, 0);
        assertTrue(liqsilDiff.phasesExistsInModel());
    }

    public static final String marsCustomDiscon = "liquid-silicate";
}
