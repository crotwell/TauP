package edu.sc.seis.TauP;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathTest {

    @Test
    public void PPath() throws TauModelException, SlownessModelException, NoArrivalException {
        String modelName = "ak135";
        TauModel tMod = TauModelLoader.load(modelName);
        SeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tMod);
        for (double deg = 35;deg <36; deg+=5) {
            List<Arrival> aList = DistanceRay.ofDegrees(deg).calculate(P_phase);
            assertEquals(1, aList.size());
            Arrival a = aList.get(0);
            assertEquals(deg, a.getDistDeg(), 1e-8);
            List<TimeDist> pierce = List.of(a.getPierce());
            TimeDist last = pierce.get(pierce.size() - 1);
            assertEquals(deg, last.getDistDeg(), 1e-8);
            assertEquals(0, last.getDepth());

            List<ArrivalPathSegment> segPath = P_phase.calcSegmentPaths(a);
            assertEquals(2, segPath.size());
            ArrivalPathSegment downgoing = segPath.get(0);
            ArrivalPathSegment upgoing = segPath.get(1);
            assertEquals(0, downgoing.getPathStart().getDepth());
            assertEquals(downgoing.getPathEnd().getDepth(), upgoing.getPathStart().getDepth());
            assertEquals(0, upgoing.getPathEnd().getDepth());

            List<TimeDist> path = List.of(a.getPath());
            TimeDist lastPath = path.get(path.size() - 1);
            assertEquals(deg, lastPath.getDistDeg(), 1e-8);
            assertEquals(0, lastPath.getDepth(), deg+" last point depth");
        }
    }
}
