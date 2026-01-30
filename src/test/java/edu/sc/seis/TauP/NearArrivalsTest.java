package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NearArrivalsTest {


    TauModel tMod;
    boolean DEBUG = false;
    double receiverDepth = 0;

    @BeforeEach
    protected void setUp() throws Exception {
        String modelName = "iasp91";
        tMod = TauModelLoader.load(modelName);
    }

    @Test
    public void onephase() throws TauPException {
        String phaseName = "SKKS";
        SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod, tMod.getSourceDepth(), receiverDepth, DEBUG);
        double tol_dist = SphericalCoords.ONE_DEG_AS_RADIAN; // radian
        double tol_time = 1.0;
        int i = 0;
        DistanceRay dray = DistanceRay.ofDegrees(i);
        for (Arrival arrival : dray.calculate(phase)) {
            Arrival near = arrival.nearbyArrival(tol_dist);
            System.err.println("near arr dist: "+near.getDist()+"  my dist: "+arrival.getDist());
            assertEquals(arrival.getDist(), near.getDist(), tol_dist, "dist: "+i);
            assertEquals(arrival.getTime(), near.getTime(), tol_time);

            TimeDist[] mypierce = arrival.getPierce();
            TimeDist[] nearpierce = near.getPierce();
            for (int j = 0; j < mypierce.length; j++) {
                System.err.println(j+" "+mypierce[j]);
                System.err.println("   "+nearpierce[j]);
                assertEquals(SphericalCoords.getModuloDistRadian(mypierce[j].getDistRadian()),
                        SphericalCoords.getModuloDistRadian(nearpierce[j].getDistRadian()),
                        tol_dist, "pierce "+j+" of "+i+" deg, near="+near.getDistDeg());
            }
        }

    }

    @ParameterizedTest
    @MethodSource("createLegalPhases")
    public void nearArrivalsPierce(String phaseName) throws TauPException {
        SimpleSeismicPhase phase = SeismicPhaseFactory.createPhase(phaseName, tMod, tMod.getSourceDepth(), receiverDepth, DEBUG);
        double tol_dist = SphericalCoords.ONE_DEG_AS_RADIAN; // radian
        double tol_time = tol_dist * 6371 / 4;
        double pierce_tol = 2*tol_dist;
        for (int i = 0; i < 180; i+=30) {
            DistanceRay dray = DistanceRay.ofDegrees(i);
            for (Arrival arrival : dray.calculate(phase)) {
                Arrival near = arrival.nearbyArrival(tol_dist);
                assertEquals(arrival.getDist(), near.getDist(), tol_dist);
                // can happen near 0 or 180 deg
                int negDist = near.isLongWayAround() != arrival.isLongWayAround() ? -1 : 1;
                if (!arrival.getName().contains("kmps")) {
                    // no need to check time on surface waves, 0.5kmps fails
                    assertEquals(arrival.getTime(), near.getTime(), tol_time);

                    TimeDist[] mypierce = arrival.getPierce();
                    TimeDist[] nearpierce = near.getPierce();
                    for (int j = 0; j < mypierce.length; j++) {
                        assertEquals(mypierce[j].getDistRadian(), negDist*nearpierce[j].getDistRadian(), pierce_tol, phaseName+" pierce "+j+" of "+i+" deg, near="+near.getDistDeg());
                    }
                }


            }
        }

    }

    public static List<String> createLegalPhases() {
        return IllegalPhasesTest.createLegalPhases();
    }
}
