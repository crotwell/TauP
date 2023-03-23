package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

public class HeadDiffWaveTest {

  @Test
  public void testHeadOrDiffractSeqSize() throws Exception {
    String modelName = "iasp91";
    double depth = 0;
    TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
    SeismicPhase pnPhase = SeismicPhaseFactory.createPhase("Pn", tMod);
    assertEquals(1, pnPhase.headOrDiffractSeq.size());
    SeismicPhase pdiffPhase = SeismicPhaseFactory.createPhase("Pdiff", tMod);
    assertEquals(1, pdiffPhase.headOrDiffractSeq.size());
    SeismicPhase pdiffpdiffPhase = SeismicPhaseFactory.createPhase("PdiffPdiff", tMod);
    assertEquals(2, pdiffpdiffPhase.headOrDiffractSeq.size());
    SeismicPhase pvmppnPhase = SeismicPhaseFactory.createPhase("PvmpPn", tMod);
    assertEquals(1, pnPhase.headOrDiffractSeq.size());
  }

  @Test
  public void testPnPierce() throws Exception {
    String modelName = "iasp91";
    double depth = 0;
    double deg = 6;
    TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
    SeismicPhase pnPhase = SeismicPhaseFactory.createPhase("Pn", tMod);

    List<Arrival> arrivals = pnPhase.calcTime(deg);
    assertEquals(1, arrivals.size());
    Arrival a = arrivals.get(0);
    TimeDist[] pierce = a.getPierce();
    assertEquals(35.0, pierce[2].getDepth());
    assertEquals(35.0, pierce[3].getDepth());
  }

  @Test
  public void testAnyDisconHeadOrDiff() throws Exception {
    String modelName = "iasp91";
    double depth = 0;
    double deg = 30;
    TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);

    int disconBranch = LegPuller.closestBranchToDepth(tMod, "410");
    SeismicPhase diffPhase = SeismicPhaseFactory.createPhase("P410diff", tMod);
    List<Arrival> arrivals = diffPhase.calcTime(deg);
    assertEquals(1, arrivals.size());
    Arrival a = arrivals.get(0);
    assertEquals(tMod.getTauBranch(disconBranch-1, true).getMinTurnRayParam(), a.getRayParam());

    SeismicPhase pnPhase = SeismicPhaseFactory.createPhase("P410n", tMod);
    List<Arrival> headarrivals = pnPhase.calcTime(deg);
    assertEquals(1, headarrivals.size());
    Arrival headArr = headarrivals.get(0);
    assertEquals(tMod.getTauBranch(disconBranch, true).getMaxRayParam(), headArr.getRayParam());
  }
}
