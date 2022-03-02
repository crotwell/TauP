package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


class IllegalPhasesTest {

	List<String> otherLegalPhases = Arrays.asList(new String[] {
			"SKviKS",
			"SKviKKviKS",
			"SK^cKS",
			"SK^cK^cKS",
			"PmPv410P",
			"ScSScSScSScS",
			"PedKP",
			"PedcS",
			"PmP^410P" });
	
	String[] illegalPhases = { "null", "ScScS", "PDDDDD", "PKIKPKIKP", "PPPdiff", 
			"PKIKIKP", "SIKS", "SKIS", "Pcv410S", "Pmv410P", "Pcv410P", "Pm^410P",
			"SKviKviKS","SK^iKS","SK^mKS", "S^S", "SVS" };
	
	// phases that are kind of wrong, but are handled by simply no arrivals, eg no ray params actually work,
	// rather than being so bad as to cause an exception
	String[] noArrivalPhases = { "PnPdiff", "scS" };

	// similar, but due to source being in mantle (below moho), these should have not ray params that
	// can generate arrivals
	String[] mantleSourceNoArrivalPhases = { "Pn", "Pvmp", "Sn", "Svms", "PmPv410P", "PmP^410P" };

	@Test
	void checkIllegalPhasesTest() throws TauModelException {
		phasesShouldFail(illegalPhases);
	}

	void phasesShouldFail(String[] phaseList) throws TauModelException {
		boolean DEBUG = true;
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		float receiverDepth = 100;
		for (String phaseName : phaseList) {
			Exception exception = assertThrows(TauModelException.class, () -> {
				SeismicPhase phase = new SeismicPhase(phaseName, tMod, receiverDepth, DEBUG);
		    }, phaseName+" shouldn't pass validation.");
		}
	}
	
	@Test
	void checkLegalPhasesTest() throws TauModelException {
		boolean DEBUG = true;
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		float receiverDepth = 100;
		List<String> legalPhases = TauP_Time.getPhaseNames("ttall");
		legalPhases.addAll(otherLegalPhases);
		for (String phaseName : legalPhases) {
			try {
				SeismicPhase phase = new SeismicPhase(phaseName, tMod, receiverDepth, DEBUG);
			} catch(TauModelException ex) {
				System.err.println("Working on phase: "+phaseName);
				throw ex;
			}
		}
	}

	@Test
	void checkNoArrivalPhasesTest() throws TauModelException {
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		double receiverDepth = 0;
		for (String phaseName : noArrivalPhases) {
			SeismicPhase phase = new SeismicPhase(phaseName, tMod, receiverDepth);
		    assertFalse(phase.phasesExistsInModel(), phaseName+" shouldn't have any ray parameters that could generate arrivals");
		}
	}

	@Test
	void checkNoArrivalMantleSourcePhasesTest() throws TauModelException {
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		double sourceDepth = tMod.getMohoDepth()+10; // 10 km below moho
		final TauModel tModDepth = tMod.depthCorrect(sourceDepth);
		double receiverDepth = 0;
		for (String phaseName : mantleSourceNoArrivalPhases) {
			SeismicPhase phase = new SeismicPhase(phaseName, tModDepth, receiverDepth);
		    assertFalse(phase.phasesExistsInModel(), phaseName+" shouldn't have arrivals for mantle source, "+sourceDepth+" phase: "+phase);
		}
	}

}
