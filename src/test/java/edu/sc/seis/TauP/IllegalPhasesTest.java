package edu.sc.seis.TauP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class IllegalPhasesTest {

	static List<String> otherLegalPhases = Arrays.asList("SKviKS",
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
			"PedKedIkp",
			"PSdiff",
			"SedPdiffKS", // literature uses "SPdiffKS", but need better name as confused with S turn in mantle, surface P to cmb then diff
			"SKdiffKiKP",
			"PPPdiff", // legal? degenerate and same as PdiffPdiffPdiff
			"PdiffKP",
			"PedPdiffKP",
			"PKdiffP",
			"PK3000diffP",// assuming discon in outer core at 3000
			"PKv3000kP",
			"PK^3000KP",
			"PKI5500diffkp", // assuming discon in inner core at 5500
			"PKIv5500ykp",
			"PKI^5500Ikp",
			"PKIkp"
	);

	static List<String> scatterLegalPhases = Arrays.asList(
			"Pop", // scattering phases
			"POp",
			"PedoP",
			"PKokp",
			"PKOKP",
			"PKIoykp",
			"PKIOIKP"
	);

	String[] illegalStartEndings = {
			"m", "c", "i", "^", "^20", "v", "v300", "V", "V300", "dif"
	};

	String[] illegalPhases = { "", "null", "blablabla should fail", "kmps",
			"ScScS", "PDDDDD", "PKIKPKIKP",
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
			"SPdiff", // S ray param > P ray param, so P leg must turn shallower than S, so can't reach cmb
			"PVcP" // oc is lvz so no crit refl

	};

	// similar, but due to source being in mantle (below moho), these should have not ray params that
	// can generate arrivals
	String[] mantleSourceNoArrivalPhases = { "Pn", "Pvmp", "Sn", "Svms", "PmPv410P", "PmP^410P" };

	ArrayList<String> createIllegalPhases() {
		List<String> legalPhases = TauP_Time.extractPhaseNames("ttall");
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
	void checkLegalPhasesTest() throws TauModelException, VelocityModelException, SlownessModelException, IOException {
		boolean DEBUG = true;

		String modelName = "outerCoreDiscon.nd";
		VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
		TauP_Create taupCreate = new TauP_Create();
		TauModel tMod_OCD = taupCreate.createTauModel(vMod);

		TauModel tModDepth = tMod_OCD.depthCorrect(10);
		float receiverDepth = 100;
		List<String> legalPhases = TauP_Time.extractPhaseNames("ttall");
		legalPhases.addAll(otherLegalPhases);
		for (String phaseName : legalPhases) {
			try {
				SeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod_OCD, tMod_OCD.getSourceDepth(), receiverDepth, DEBUG);
				SeismicPhase phase_depth = SeismicPhaseFactory.createPhase(phaseName, tModDepth, tModDepth.getSourceDepth(), receiverDepth, DEBUG);
			} catch(TauModelException ex) {
				System.err.println("Working on phase: "+phaseName);
				throw ex;
			}
		}
	}

	@Test
	void checkLegalScatterPhasesTest() throws TauModelException, VelocityModelException, SlownessModelException, IOException {
		boolean DEBUG = true;
		// maybe dumb test, just check that leg puller doesn't die
		List<String> legalPhases = TauP_Time.extractPhaseNames("ttall");
		legalPhases.addAll(scatterLegalPhases);
		for (String phaseName : legalPhases) {
			List<String> legs = LegPuller.legPuller(phaseName);
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
