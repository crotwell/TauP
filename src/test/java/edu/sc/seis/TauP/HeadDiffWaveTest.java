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
    SeismicPhase pnPhase = new SeismicPhase("Pn", tMod);
    assertEquals(1, pnPhase.headOrDiffractSeq.size());
    SeismicPhase pdiffPhase = new SeismicPhase("Pdiff", tMod);
    assertEquals(1, pdiffPhase.headOrDiffractSeq.size());
    SeismicPhase pdiffpdiffPhase = new SeismicPhase("PdiffPdiff", tMod);
    assertEquals(2, pdiffpdiffPhase.headOrDiffractSeq.size());
    SeismicPhase pvmppnPhase = new SeismicPhase("PvmpPn", tMod);
    assertEquals(1, pnPhase.headOrDiffractSeq.size());
  }

  @Test
  public void testPnPierce() throws Exception {
    String modelName = "iasp91";
    double depth = 0;
    double deg = 6;
    TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
    SeismicPhase pnPhase = new SeismicPhase("Pn", tMod);

    List<Arrival> arrivals = pnPhase.calcTime(deg);
    assertEquals(1, arrivals.size());
    Arrival a = arrivals.get(0);
    TimeDist[] pierce = a.getPierce();
    assertEquals(35.0, pierce[2].getDepth());
    assertEquals(35.0, pierce[3].getDepth());
  }
}
