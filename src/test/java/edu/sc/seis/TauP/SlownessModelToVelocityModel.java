package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SlownessModelToVelocityModel {

    /**
     * Test if the velocity model and slowness model are the same at all depth, skipping zero thickness
     * slowness layers.
     * 
     * @throws TauModelException
     * @throws NoSuchLayerException
     * @throws NoSuchMatPropException
     */
    @ParameterizedTest
    @ValueSource(strings = {"iasp91", "ak135", "prem"})
    void sModTovMod(String modelName) throws TauModelException, NoSuchLayerException, NoSuchMatPropException {

        TauModel tMod = TauModelLoader.load(modelName);
        SlownessModel sMod = tMod.getSlownessModel();
        VelocityModel vMod = sMod.getVelocityModel();
        for (SlownessLayer sLayer: sMod.getAllSlownessLayers(true)) {
            if (sLayer.getTopDepth() != sLayer.getBotDepth()) {
                // there will be zero thickness slowness layers that are in between
                // velocity values
                double topcalcVel = (vMod.radiusOfEarth - sLayer.getTopDepth())
                        / sLayer.getTopP();
                assertEquals(vMod.evaluateBelow(sLayer.getTopDepth(), 'P'), topcalcVel, 0.01, "P at "+sLayer.getTopDepth()+" "+sLayer);
                if (sLayer.getBotDepth() != sMod.getRadiusOfEarth()) {
                    double botcalcVel = (vMod.radiusOfEarth - sLayer.getBotDepth())
                            / sLayer.getBotP();
                    assertEquals(vMod.evaluateAbove(sLayer.getBotDepth(), 'P'), botcalcVel, 0.01, "P at "+sLayer.getBotDepth()+" "+sLayer);
                }
            }
        }
        for (SlownessLayer sLayer: sMod.getAllSlownessLayers(false)) {
            if (sMod.depthInFluid(sLayer.getTopDepth())) {
                // skip fluid layers
                continue;
            } else if (sLayer.getTopDepth() != sLayer.getBotDepth()) {
                // there will be zero thickness slowness layers that are in between
                // velocity values
                double calcVel = (vMod.radiusOfEarth - sLayer.getTopDepth())
                        / sLayer.getTopP();
                assertEquals(vMod.evaluateBelow(sLayer.getTopDepth(), 'S'), calcVel, 0.01, "S at "+sLayer.getTopDepth());
                if (sLayer.getBotDepth() != sMod.getRadiusOfEarth()) {
                    double botcalcVel = (vMod.radiusOfEarth - sLayer.getBotDepth())
                            / sLayer.getBotP();
                    assertEquals(vMod.evaluateAbove(sLayer.getBotDepth(), 'S'), botcalcVel, 0.01, "S at "+sLayer.getBotDepth()+" "+sLayer);
                }
            }
        }
    }


    /**
     * Test if the velocity model and slowness model are the same at all depth, skipping zero thickness
     * slowness layers.
     * 
     * @throws TauModelException
     * @throws NoSuchLayerException
     * @throws NoSuchMatPropException
     */
    @ParameterizedTest
    @ValueSource(strings = {"iasp91", "ak135", "prem"})
    void vModTosMod(String modelName) throws TauModelException, NoSuchLayerException, NoSuchMatPropException {

        TauModel tMod = TauModelLoader.load(modelName);
        SlownessModel sMod = tMod.getSlownessModel();
        VelocityModel vMod = sMod.getVelocityModel();
        for (VelocityLayer vLayer: vMod.getLayers()) {
            if (sMod.depthInFluid(vLayer.getTopDepth())) {
                // skip fluid layers
                continue;
            } else {
                boolean isPWave = true;
                double topcalcRP = (vMod.radiusOfEarth - vLayer.getTopDepth())
                        / vLayer.getTopPVelocity();
                SlownessLayer sLayer = sMod.getSlownessLayer(sMod.layerNumberBelow(vLayer.getTopDepth(), isPWave), isPWave);
                assertEquals(sLayer.getTopP(), topcalcRP, 0.00001, " top ray param "+vLayer);
                double botcalcRP = (vMod.radiusOfEarth - vLayer.getBotDepth())
                        / vLayer.getBotPVelocity();
                SlownessLayer botsLayer = sMod.getSlownessLayer(sMod.layerNumberAbove(vLayer.getBotDepth(), isPWave), isPWave);
                assertEquals(botsLayer.getBotP(), botcalcRP, 0.00001, " bot ray param "+vLayer);

                isPWave = false;
                topcalcRP = (vMod.radiusOfEarth - vLayer.getTopDepth())
                        / vLayer.getTopSVelocity();
                sLayer = sMod.getSlownessLayer(sMod.layerNumberBelow(vLayer.getTopDepth(), isPWave), isPWave);
                assertEquals(sLayer.getTopP(), topcalcRP, 0.00001, " top ray param "+vLayer);
                botcalcRP = (vMod.radiusOfEarth - vLayer.getBotDepth())
                        / vLayer.getBotSVelocity();
                botsLayer = sMod.getSlownessLayer(sMod.layerNumberAbove(vLayer.getBotDepth(), isPWave), isPWave);
                assertEquals(botsLayer.getBotP(), botcalcRP, 0.00001, " bot ray param "+vLayer);

            }
        }
    }

    /**
     * Test if the velocity model and slowness model are the same at all depth, skipping zero thickness
     * slowness layers.
     * 
     * @throws TauModelException
     * @throws NoSuchLayerException
     * @throws NoSuchMatPropException
     * @throws SlownessModelException 
     */
    @ParameterizedTest
    @ValueSource(strings = {"iasp91", "ak135", "prem"})
    void sModCenterRayParam(String modelName) throws TauModelException, NoSuchLayerException, NoSuchMatPropException, SlownessModelException {

        TauModel tMod = TauModelLoader.load(modelName);
        SlownessModel sMod = tMod.getSlownessModel();
        sMod.validate();
        SlownessLayer centerLayer = sMod.getSlownessLayer(sMod.getNumLayers(true)-1, true);
        assertEquals(0, centerLayer.getBotP(), "center zero ray param: "+centerLayer);
    }
}
