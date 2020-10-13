package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class IllegalPhasesTest {

	String[] illegalPhases = { "PKIKPKIKP", "PnPdiff" };

	@Test
	void checkIllegalPhasesTest() throws TauModelException {
		String modelName = "iasp91";
		TauModel tMod = TauModelLoader.load(modelName);
		float receiverDepth = 100;
		for (String phaseName : illegalPhases) {
			try {
				SeismicPhase phase = new SeismicPhase(phaseName, tMod, receiverDepth);
				fail("Phase '"+phaseName+"' should not be allowed");
			} catch (TauModelException e) {

			}
		}
	}

}
