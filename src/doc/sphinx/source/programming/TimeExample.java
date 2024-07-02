package edu.sc.seis.example;

import edu.sc.seis.TauP.*;

import java.io.IOException;
import java.util.List;

public class TimeExample {

    public void calcTimes() throws TauPException, IOException {
        // A standard model can be loaded by name
        TauModel ak135Model = TauModelLoader.load("ak135");
        // or a custom velocity model, from a file in current directory, can be
        // loaded and then turned into a TauModel
        VelocityModel vMod = TauModelLoader.loadVelocityModel("mymodel.nd");
        TauP_Create tauPCreate = new TauP_Create();
        TauModel tMod = tauPCreate.createTauModel(vMod);

        // A seismic phase for a phase name like 'P' can be created for that model
        double sourceDepth = 100;  // earthquake depth in kilometers
        double receiverDepth = 0;  // seismometer depth in kilometers if not at the surface
        SeismicPhase P_phase = SeismicPhaseFactory.createPhase("P", tMod, sourceDepth);

        //
        List<Arrival> arrivalList = DistanceRay.ofDegrees(45).calculate(P_phase);
        for (Arrival a : arrivalList) {
            System.out.println(a.getName()+" "+a.getDistDeg()+" "+a.getTime());
        }
    }

    public static void main(String[] args) throws Exception {
        TimeExample ex = new TimeExample();
        ex.calcTimes();
    }
}
