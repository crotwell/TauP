package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PuristNameTest {

    @Test
    public void puristNamePdiff() throws TauModelException {
        String Pdiff = "Pdiff";
        TauModel tauModel = TauModelLoader.load("ak135fcont");
        SeismicPhase sp = SeismicPhaseFactory.createPhase(Pdiff, tauModel);
        assertTrue(sp.phasesExistsInModel());
        assertEquals(Pdiff, sp.getPuristName());
    }

    @Test
    public void puristNameP20P() throws TauModelException {
        List<List<String>> names = new ArrayList<>();
        names.add(List.of("P20P", "Ped20P"));
        names.add(List.of("P20p", "P20p"));
        names.add(List.of("P20S", "Ped20S"));
        names.add(List.of("P20s", "P20s"));

        TauModel tauModel = TauModelLoader.load("ak135fcont");
        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel());
            assertEquals(ph.get(1), sp.getPuristName(), ph.get(0)+" -> "+ph.get(1));
        }
    }

    @Test
    public void puristName() throws TauModelException {
        List<List<String>> names = new ArrayList<>();
        names.add(List.of("P660diff^410P", "P660diff^410P"));
        names.add(List.of("P660diff^410P660diff", "P660diff^410P660diff"));
        names.add(List.of("PKI5154nkp", "PKI5154nkp"));

        TauModel tauModel = TauModelLoader.load("ak135fcont");
        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel());
            assertEquals(ph.get(1), sp.getPuristName(), ph.get(0)+" -> "+ph.get(1));
        }
    }
    @Test
    public void puristName_PKI5154nkp() throws TauModelException {
        List<List<String>> names = new ArrayList<>();
        names.add(List.of("PKI5154nkp", "PKI5154nkp"));

        TauModel tauModel = TauModelLoader.load("ak135fcont");
        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel(), "Doesn't exist? "+sp.failReason());
            assertEquals(ph.get(1), sp.getPuristName(), ph.get(0)+" -> "+ph.get(1));
        }
    }

    @Test
    public void otherDiffDownTest() throws Exception {
        String phasename = "P1554diffdnPcp";
        String pureName = "P1554diffdnPcp";
        String modelName = "MarsLiquidLowerMantle.nd";
        VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
        TauModel tauModel = TauModelLoader.createTauModel(vMod);
        SeismicPhase sp = SeismicPhaseFactory.createPhase(phasename, tauModel);

        assertTrue(sp.phasesExistsInModel());
        assertEquals(pureName, sp.getPuristName(), phasename+" -> "+pureName);
    }
    @Test
    public void SedPDiffDownTest() throws Exception {
        String phasename = "SedPdiffdnKP";
        String pureName = "SedPdiffdnKp";
        String modelName = "ak135fcont";
        TauModel tauModel = TauModelLoader.load(modelName);
        SeismicPhase sp = SeismicPhaseFactory.createPhase(phasename, tauModel);

        assertTrue(sp.phasesExistsInModel());
        assertEquals(pureName, sp.getPuristName(), phasename+" -> "+pureName);
    }

    @Test
    public void normalNames() throws Exception {
        List<List<String>> names = new ArrayList<>();
        names.add(List.of("PvmP", "Pvmp"));
        // ttall
        names.add(List.of("p", "p"));
        names.add(List.of("s", "s"));
        names.add(List.of("P", "P"));
        names.add(List.of("S", "S"));
        names.add(List.of("Pn", "Pn"));
        names.add(List.of("Sn", "Sn"));
        names.add(List.of("PcP", "Pcp"));
        names.add(List.of("ScS", "Scs"));
        names.add(List.of("Pdiff", "Pdiff"));
        names.add(List.of("Sdiff", "Sdiff"));
        names.add(List.of("PKP", "PKp"));
        names.add(List.of("SKS", "SKs"));
        names.add(List.of("PKiKP", "PKikp"));
        names.add(List.of("SKiKS", "SKiks"));
        names.add(List.of("PKIKP", "PKIkp"));
        names.add(List.of("SKIKS", "SKIks"));
        names.add(List.of("PcP", "Pcp"));
        names.add(List.of("pP", "pP"));
        names.add(List.of("pPdiff", "pPdiff"));
        names.add(List.of("pPKP", "pPKp"));
        names.add(List.of("pPKIKP", "pPKIkp"));
        names.add(List.of("sP", "sP"));
        names.add(List.of("sPdiff", "sPdiff"));
        names.add(List.of("sPKP", "sPKp"));
        names.add(List.of("sPKIKP", "sPKIkp"));
        names.add(List.of("sPKiKP", "sPKikp"));
        names.add(List.of("sS", "sS"));
        names.add(List.of("sSdiff", "sSdiff"));
        names.add(List.of("sSKS", "sSKs"));
        names.add(List.of("sSKIKS", "sSKIks"));
        names.add(List.of("ScS", "Scs"));
        names.add(List.of("pS", "pS"));
        names.add(List.of("pSdiff", "pSdiff"));
        names.add(List.of("pSKS", "pSKs"));
        names.add(List.of("sSKIKS", "sSKIks"));
        names.add(List.of("ScP", "Scp"));
        names.add(List.of("SKP", "SKp"));
        names.add(List.of("SKIKP", "SKIkp"));
        names.add(List.of("PKKP", "PKKp"));
        names.add(List.of("PKIKKIKP", "PKIkKIkp"));
        names.add(List.of("SKKP", "SKKp"));
        names.add(List.of("SKIKKIKP", "SKIkKIkp"));
        names.add(List.of("PP", "PP"));
        names.add(List.of("PKPPKP", "PKpPKp"));
        names.add(List.of("PKIKPPKIKP", "PKIkpPKIkp"));
        names.add(List.of("SKiKP", "SKikp"));
        names.add(List.of("PP", "PP"));
        names.add(List.of("ScS", "Scs"));
        names.add(List.of("PcS", "Pcs"));
        names.add(List.of("PKS", "PKs"));
        names.add(List.of("PKIKP", "PKIkp"));
        names.add(List.of("PKKS", "PKKs"));
        names.add(List.of("PKIKKIKS", "PKIkKIks"));
        names.add(List.of("SKKS", "SKKs"));
        names.add(List.of("SKIKKIKS", "SKIkKIks"));
        names.add(List.of("SKSSKS", "SKsSKs"));


        String DEFAULT_PHASES = "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS";
        String ttall = DEFAULT_PHASES
                +",PcP,pP,pPdiff,pPKP,pPKIKP,sP,sPdiff,sPKP,sPKIKP,sPKiKP"
                +",sS,sSdiff,sSKS,sSKIKS,ScS,pS,pSdiff,pSKS,sSKIKS"
                +",ScP,SKP,SKIKP,PKKP,PKIKKIKP,SKKP,SKIKKIKP,PP,PKPPKP,PKIKPPKIKP"
                +",SKiKP,PP,ScS,PcS,PKS,PKIKP,PKKS,PKIKKIKS,SKKS,SKIKKIKS,SKSSKS";
        /*
        String[] normalPhases = ttall.split(",");
        for (String p : normalPhases) {
            String pure = p;
            names.add(List.of(p, p));
        }
        */




        TauModel tauModel = TauModelLoader.load("ak135fcont").depthCorrect(10);
        StringBuilder sb = new StringBuilder();
        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel(), ph.get(0));
                sb.append("names.add(List.of(\""+ph.get(1)+"\", \""+sp.getPuristName()+"\"));\n");
        }
        System.err.println("##################");
        System.err.println(sb);

        for (List<String> ph : names) {
            SeismicPhase sp = SeismicPhaseFactory.createPhase(ph.get(0), tauModel);
            assertTrue(sp.phasesExistsInModel(), ph.get(0));
            assertEquals(ph.get(1), sp.getPuristName(), ph.get(0)+" -> "+ph.get(1)+"  "+sp.describe());
        }
    }


}
