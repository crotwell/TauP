package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class SetSacTest {

    @Test
    public void testSetSacFile() throws TauPException, SetSacException {
        SacHeader header = new SacHeader();
        header.setEvla(40.6890f);
        header.setEvlo(-74.7540f);
        header.setEvdp(4.7f);
        header.setStla(34.14f);
        header.setStlo(-80.69f);
        // calc from iris distaz web service:
        // https://service.iris.edu/irisws/distaz/1/query?stalat=34.14&stalon=-80.69&evtlat=40.6890&evtlon=-74.7540
        // for New Jersey event:
        // us7000ma74  2024-04-05T14:23:20Z  40.6890/-74.7540  4.7 km  4.8 mwr
        // to CO.BARN  34.14/-80.69
        //
        header.setGcarc(8.06954f);
        header.setDist(896.3953f);
        header.setAz(217.7121f);
        header.setBaz(34.09757f);
        float oMarker = 10f;
        header.setO(oMarker);
        SacTimeSeries sac = new SacTimeSeries(header);
        TauP_SetSac setsac = new TauP_SetSac();
        setsac.phaseNames = new ArrayList<>();
        PhaseName phaseP = new PhaseName("P", 1);
        setsac.phaseNames.add(phaseP);
        PhaseName phaseS = new PhaseName("S", "234");
        setsac.phaseNames.add(phaseS);
        setsac.sacFileNames = new ArrayList<>();
        setsac.init();
        setsac.processSacTimeSeries(sac, "inmemory");
        assertEquals("P", sac.getHeader().getKt1());
        assertEquals(118.43+oMarker, sac.getHeader().getT1(), 0.01);
        assertEquals("S", sac.getHeader().getKt2());
        assertEquals(211.62+oMarker, sac.getHeader().getT2(), 0.01);
        assertEquals("S", sac.getHeader().getKt3());
        assertEquals(243.70+oMarker, sac.getHeader().getT3(), 0.01);
        assertEquals("S", sac.getHeader().getKt4());
        assertEquals(243.70+oMarker, sac.getHeader().getT4(), 0.01);

    }
}
