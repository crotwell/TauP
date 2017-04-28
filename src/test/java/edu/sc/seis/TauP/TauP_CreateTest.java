package edu.sc.seis.TauP;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;


public class TauP_CreateTest {

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testGetCmbDepth() throws IOException, VelocityModelException, SlownessModelException, TauModelException {
        TauP_Create taupCreate = new TauP_Create();
        for (String modelName : VelocityModelTest.modelNames) {
            VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
            TauModel tMod = taupCreate.createTauModel(vMod);
            assertEquals("moho depth", vMod.getMohoDepth(), tMod.getMohoDepth(), 0.000001);
            assertEquals("cmb", vMod.getCmbDepth(), tMod.getCmbDepth(), 0.000001);
            assertEquals("iocb depth", vMod.getIocbDepth(), tMod.getIocbDepth(), 0.000001);
        }
    }
}
