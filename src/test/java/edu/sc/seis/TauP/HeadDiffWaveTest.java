package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeadDiffWaveTest {

  @Test
  public void testHeadOrDiffractSeqSize() throws Exception {
    double depth = 0;
    TauModel tMod = TauModelLoader.load(modelName).depthCorrect(depth);
    SimpleSeismicPhase pnPhase = SeismicPhaseFactory.createPhase("Pn", tMod);
    assertEquals(1, pnPhase.headOrDiffractSeq.size());
    SimpleSeismicPhase pdiffPhase = SeismicPhaseFactory.createPhase("Pdiff", tMod);
    assertEquals(1, pdiffPhase.headOrDiffractSeq.size());
    SimpleSeismicPhase pdiffpdiffPhase = SeismicPhaseFactory.createPhase("PdiffPdiff", tMod);
    assertEquals(2, pdiffpdiffPhase.headOrDiffractSeq.size());
    SeismicPhase pvmppnPhase = SeismicPhaseFactory.createPhase("PvmpPn", tMod);
    assertEquals(1, pnPhase.headOrDiffractSeq.size());
  }

  @Test
  public void testPnPierce() throws Exception {
    double depth = 0;
    double deg = 6;
    TauModel tModDepth = tMod.depthCorrect(depth);
    SeismicPhase pnPhase = SeismicPhaseFactory.createPhase("Pn", tModDepth);

    List<Arrival> arrivals = pnPhase.calcTime(deg);
    assertEquals(1, arrivals.size());
    Arrival a = arrivals.get(0);
    TimeDist[] pierce = a.getPierce();
    assertEquals(35.0, pierce[2].getDepth());
    assertEquals(35.0, pierce[3].getDepth());
  }

  @Test
  public void testAnyDisconHeadOrDiff() throws Exception {
    double depth = 0;
    double deg = 30;
    TauModel tModDepth = tMod.depthCorrect(depth);

  }
  @Test
  public void test_P410diff() throws TauModelException {
    double depth = 0;
    double deg = 30;
    TauModel tModDepth = tMod.depthCorrect(depth);

    int disconBranch = LegPuller.closestBranchToDepth(tModDepth, "410");
    SeismicPhase diffPhase = SeismicPhaseFactory.createPhase("P410diff", tModDepth);
    List<Arrival> arrivals = diffPhase.calcTime(deg);
    assertEquals(1, arrivals.size());
    Arrival a = arrivals.get(0);
    assertEquals(tMod.getTauBranch(disconBranch - 1, true).getMinTurnRayParam(), a.getRayParam());
    SeismicPhase reflPhase = SeismicPhaseFactory.createPhase("Pv410p", tModDepth);
    // greatest reflection dist should match smallest diff distance
    assertEquals(reflPhase.getMaxDistance(), diffPhase.getMinDistance(), 0.00001);
    Arrival minDiffArrival = diffPhase.getEarliestArrival(diffPhase.getMinDistanceDeg());
    Arrival reflArrival = reflPhase.getEarliestArrival(reflPhase.getMaxDistanceDeg());
    assertEquals(reflArrival.getTime(), minDiffArrival.getTime(), 0.00001);
    assertEquals(reflArrival.getDist(), minDiffArrival.getDist(), 0.00001);
    assertEquals(reflArrival.getRayParam(), minDiffArrival.getRayParam(), 0.00001);

  }
  @Test
  public void test_P410n() throws TauModelException {
    double depth = 0;
    double deg = 30;
    TauModel tModDepth = tMod.depthCorrect(depth);
    int disconBranch = LegPuller.closestBranchToDepth(tModDepth, "410");
    SeismicPhase headPhase = SeismicPhaseFactory.createPhase("P410n", tModDepth);
    List<Arrival> headarrivals = headPhase.calcTime(deg);
    assertEquals(1, headarrivals.size());
    Arrival headArr = headarrivals.get(0);
    assertEquals(tMod.getTauBranch(disconBranch, true).getMaxRayParam(), headArr.getRayParam());

    SeismicPhase reflPhase = SeismicPhaseFactory.createPhase("PV410p", tModDepth);
    // smallest crit reflection dist should match smallest head distance
    assertEquals(reflPhase.getMinDistance(), headPhase.getMinDistance(), 0.00001);
    Arrival minHeadArrival = headPhase.getEarliestArrival(reflPhase.getMinDistanceDeg());
    Arrival reflArrival = reflPhase.getEarliestArrival(reflPhase.getMinDistanceDeg());
    assertEquals(reflArrival.getTime(), minHeadArrival.getTime(), 0.00001);
    assertEquals(reflArrival.getDist(), minHeadArrival.getDist(), 0.00001);
    assertEquals(reflArrival.getRayParam(), minHeadArrival.getRayParam(), 0.00001);
  }
  @Test
  public void test_SedPdiff() throws TauModelException {
    double depth = 0;
    double deg = 90;
    TauModel tModDepth = tMod.depthCorrect(depth);
    SeismicPhase SedPdiff_Phase = SeismicPhaseFactory.createPhase("SedPdiff", tModDepth);
    List<Arrival> SedPdiff_arrivals = SedPdiff_Phase.calcTime(deg);
    assertEquals(1, SedPdiff_arrivals.size());
    Arrival SedPdiff_Arr = SedPdiff_arrivals.get(0);
    assertEquals(tMod.getTauBranch(tMod.getCmbBranch()-1, true).getMinTurnRayParam(), SedPdiff_Arr.getRayParam());
  }

  @Test
  public void test_SedPdiffKP() throws TauModelException {
    double deg = 150;
    SeismicPhase SedPdiff_Phase = SeismicPhaseFactory.createPhase("SedPdiffKs", tMod);
    List<Arrival> SedPdiff_arrivals = SedPdiff_Phase.calcTime(deg);
    assertEquals(1, SedPdiff_arrivals.size());
    Arrival SedPdiff_Arr = SedPdiff_arrivals.get(0);
    assertEquals(tMod.getTauBranch(tMod.getCmbBranch()-1, true).getMinTurnRayParam(), SedPdiff_Arr.getRayParam());
    SeismicPhase SKPdiffs_Phase = SeismicPhaseFactory.createPhase("SKPdiffs", tMod);
    List<Arrival> SKPdiffs_arrivals = SKPdiffs_Phase.calcTime(deg);
    assertEquals(1, SKPdiffs_arrivals.size());
    Arrival SKPdiffs_Arr = SKPdiffs_arrivals.get(0);
    assertEquals(tMod.getTauBranch(tMod.getCmbBranch()-1, true).getMinTurnRayParam(), SKPdiffs_Arr.getRayParam());
    assertEquals(SedPdiff_Arr.getTime(), SKPdiffs_Arr.getTime());
  }

  @Test
  public void test_PK3000diffP() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
    String modelName = "outerCoreDiscon.nd";
    VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
    TauP_Create taupCreate = new TauP_Create();
    TauModel tMod_OCD = taupCreate.createTauModel(vMod);
    double deg = 150;
    SeismicPhase SedPdiff_Phase = SeismicPhaseFactory.createPhase("PK3000diffP", tMod_OCD);
    List<Arrival> SedPdiff_arrivals = SedPdiff_Phase.calcTime(deg);
    assertEquals(1, SedPdiff_arrivals.size());
    Arrival SedPdiff_Arr = SedPdiff_arrivals.get(0);

    int disconBranch = LegPuller.closestBranchToDepth(tMod, "3000");
    assertEquals(tMod_OCD.getTauBranch(disconBranch-1, true).getMinTurnRayParam(), SedPdiff_Arr.getRayParam());

}



  @Test
  public void test_PK5500diffP() throws VelocityModelException, SlownessModelException, TauModelException, IOException {
    // outerCoreDiscon has discon at 3000 in outer core and at 5500 in inner core
    String modelName = "outerCoreDiscon.nd";
    VelocityModel vMod = VelocityModelTest.loadTestVelMod(modelName);
    TauP_Create taupCreate = new TauP_Create();
    TauModel tMod_OCD = taupCreate.createTauModel(vMod);
    double deg = 150;
    SeismicPhase ic_diff_Phase = SeismicPhaseFactory.createPhase("PKI5500diffKP", tMod_OCD);
    List<Arrival> ic_diff_arrivals = ic_diff_Phase.calcTime(deg);
    assertEquals(1, ic_diff_arrivals.size());
    Arrival ic_diff_Arr = ic_diff_arrivals.get(0);

    int disconBranch = LegPuller.closestBranchToDepth(tMod_OCD, "5500");
    TauBranch tBranch = tMod_OCD.getTauBranch(disconBranch-1, true);
    assertEquals(5500, tBranch.getBotDepth());
    assertEquals(tBranch.getMinTurnRayParam(), ic_diff_Arr.getRayParam());

    // also test reflection
    SeismicPhase reflectPhase = SeismicPhaseFactory.createPhase("PKIv5500ykp", tMod_OCD);
    Arrival reflectArrr = reflectPhase.getEarliestArrival(25);
    assertNotNull(reflectArrr);
  }


  @BeforeAll
  public static void createTMod() throws TauModelException {
    tMod = TauModelLoader.load(modelName);
  }

  static String modelName = "iasp91";
  static TauModel tMod;
}
