package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MarsTest {


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
        assertTrue(PhaseSymbols.isDiffracted(Pdiff, 0), "isDiffracted " + Pdiff + " re: " + LegPuller.namedHeadDiffRE);

        String liqsilDiffName = "P" + PhaseSymbols.NAMED_DISCON_START + marsCustomDiscon + PhaseSymbols.NAMED_DISCON_END + "diff";
        assertTrue(PhaseSymbols.isDiffracted(liqsilDiffName, 0), "isDiffracted " + liqsilDiffName);
        SeismicPhase liqsilDiff = SeismicPhaseFactory.createPhase(liqsilDiffName, tMod, 0, 0);
        assertTrue(liqsilDiff.phasesExistsInModel());

        String liqsilDiffDownName = "P" + PhaseSymbols.NAMED_DISCON_START + marsCustomDiscon + PhaseSymbols.NAMED_DISCON_END + "diffdnPcp";
        assertTrue(PhaseSymbols.isDiffractedDown(liqsilDiffDownName, 0), "isDiffracted down " + liqsilDiffName);
        SeismicPhase liqsilDiffDown = SeismicPhaseFactory.createPhase(liqsilDiffDownName, tMod, 0, 0);
        assertTrue(liqsilDiffDown.phasesExistsInModel());
    }

    @Test
    public void marsMay25LiquidLowerMantleDiffdown() throws VelocityModelException, IOException, SlownessModelException, TauModelException {
        VelocityModel marsVMod = VelocityModelTest.loadTestVelMod("MarsKahnMay2025.nd");
        assertEquals(0.0, marsVMod.getVelocityLayer(marsVMod.layerNumberAbove(1560)).getTopSVelocity());
        TauModel tMod = TauModelLoader.createTauModel(marsVMod);
        String oddDiffDownPhase = "PcpP1540diffdnPcp";
        SeismicPhase oddDiffDownSP = SeismicPhaseFactory.createPhase(oddDiffDownPhase, tMod, 0, 0);
        assertTrue(oddDiffDownSP.phasesExistsInModel());

    }

    public static final String marsCustomDiscon = "liquid-silicate";
}
