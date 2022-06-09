package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


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
			"Pedcs",
			"PmP^410P",
			"Pedcp",
			"PdiffKP",
			"PedPdiffKP",
			"PKp",
			"PKs",
			"Pn^20P",
			"Pdiff^410P",
			"PK^cKP",
			"PKI^iIKP",
			"Pvmp",
			"PVmp",
			"Pvcp",
			"PVcp",
			"PKviKP",
			"PKViKP",
			"2kmps", "2.5kmps", ".5kmps", "10kmps",
			"PKedikp",
			"PedKedIkp"
	});

	String[] illegalStartEndings = {
			"m", "c", "i", "^", "^20", "v", "v300", "V", "V300", "dif"
	};

	String[] illegalPhases = { "", "null", "blablabla should fail", "kmps",
			"ScScS", "PDDDDD", "PKIKPKIKP", "PPPdiff",
			"PKIKIKP", "SIKS", "SKIS", "Pcv410S", "Pmv410P", "Pcv410P", "Pm^410P",
			"SKviKviKS","SK^iKS","SK^mKS", "S^S", "SVS", "Pdiffdiff", "SVS", "SccS",
			"SIKS", "Siks", "SiKS", "Kdiff", "pp", "sp", "ss", "Ss", "Ps", "Sp", "Pp",
			"scS", "pcP", "kiKP", "kkP", "iKP",
			"Icp", "P^iP", "P^", "Pv", "PV", "k^mP",
			"k^iKP", "P300", "PK3500", "PKI5500", "Pv410", "PKv", "PKV", "PK^", "Pdif",
			"PVi", "Pvi", "PKPab", "PKPbc", "PKPdf"
	};

	// phases that are kind of wrong, but are handled by simply no arrivals, eg no ray params actually work,
	// rather than being so bad as to cause an exception
	String[] noArrivalPhases = { "PnPdiff",
			"PdiffSdiff",
			"PVcP" // oc is lvz so no crit refl
	};

	// similar, but due to source being in mantle (below moho), these should have not ray params that
	// can generate arrivals
	String[] mantleSourceNoArrivalPhases = { "Pn", "Pvmp", "Sn", "Svms", "PmPv410P", "PmP^410P" };

	ArrayList<String> createIllegalPhases() {
		List<String> legalPhases = TauP_Time.getPhaseNames("ttall");
		ArrayList<String> illegalEndPhases = new ArrayList<>();
		legalPhases.addAll(otherLegalPhases);
		for (String phaseName : legalPhases) {
			for (String illEnd : illegalStartEndings) {
				illegalEndPhases.add(phaseName+illEnd);
				illegalEndPhases.add(illEnd+phaseName);
			}
		}
		illegalEndPhases.addAll(Arrays.asList(illegalPhases));
		return illegalEndPhases;
	}

	@Test
	void checkIllegalPhasesTest() throws TauModelException {
		phasesShouldFail(createIllegalPhases());
	}

	void phasesShouldFail(List<String> phaseList) throws TauModelException {
		boolean DEBUG = true;
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		TauModel tModDepth = tMod.depthCorrect(10);
		float receiverDepth = 100;
		for (String phaseName : phaseList) {
			// either throws or has no arrivals, maxRayParam == -1
			try {
				SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod, tMod.getSourceDepth(), receiverDepth, DEBUG);
				assertFalse(phase.phasesExistsInModel(), phaseName + " shouldn't pass validation, source: " + tModDepth.getSourceDepth());
			} catch(TauModelException e) {
				// ok
			}
			try {
				SeismicPhase phase_depth = SeismicPhaseFactory.createPhase(phaseName, tModDepth, tModDepth.getSourceDepth(), receiverDepth, DEBUG);
				assertFalse(phase_depth.phasesExistsInModel(), phaseName + " shouldn't pass validation, source: " + tModDepth.getSourceDepth());
			} catch(TauModelException e) {
				// ok
			}
		}
	}
	
	@Test
	void checkLegalPhasesTest() throws TauModelException {
		boolean DEBUG = true;
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		TauModel tModDepth = tMod.depthCorrect(10);
		float receiverDepth = 100;
		List<String> legalPhases = TauP_Time.getPhaseNames("ttall");
		legalPhases.addAll(otherLegalPhases);
		for (String phaseName : legalPhases) {
			try {
				SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod, tMod.getSourceDepth(), receiverDepth, DEBUG);
				SeismicPhase phase_depth = SeismicPhaseFactory.createPhase(phaseName, tModDepth, tModDepth.getSourceDepth(), receiverDepth, DEBUG);
			} catch(TauModelException ex) {
				System.err.println("Working on phase: "+phaseName);
				throw ex;
			}
		}
	}

	@Test
	void checkNoArrivalPhasesTest() throws TauModelException {
		boolean DEBUG = false;
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		TauModel tModDepth = tMod.depthCorrect(10);
		double receiverDepth = 0;
		for (String phaseName : noArrivalPhases) {
			SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod, tMod.getSourceDepth(), receiverDepth, DEBUG);
		    assertFalse(phase.phasesExistsInModel(), phaseName+" shouldn't have any ray parameters that could generate arrivals");
			SeismicPhase phase_depth = SeismicPhaseFactory.createPhase(phaseName, tModDepth, tModDepth.getSourceDepth(), receiverDepth, DEBUG);
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
			SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tModDepth, sourceDepth, receiverDepth);
		    assertFalse(phase.phasesExistsInModel(), phaseName+" shouldn't have arrivals for mantle source, "+sourceDepth+" phase: "+phase);
		}
	}

}
