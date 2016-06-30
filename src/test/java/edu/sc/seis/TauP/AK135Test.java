package edu.sc.seis.TauP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

/** test data from http://rses.anu.edu.au/~brian/AK135tables.pdf
 */
public class AK135Test extends TestCase { 

    protected void setUp() throws Exception {
        taup = new TauP_Time("ak135");
        loadTable();
    }
    
    public void loadTable() throws Exception {
        table = new HashMap<String, HashMap<Float, List<TimeDist>>>();
        readTable("ak135_P_shallow.txt", "P");
        readTable("ak135_P_deep.txt", "P");
        readTable("ak135_S_shallow.txt", "S");
        readTable("ak135_S_deep.txt", "S");
        readTable("ak135_PcP.txt", "PcP");
        readTable("ak135_ScS.txt", "ScS");
        readTable("ak135_ScP.txt", "ScP");
        readTable("ak135_PKIKP.txt", "PKIKP");
    }
    
    public HashMap<String, HashMap<Float, List<TimeDist>>> getTable() {
        return table;
    }

    @Test
    public void testTableP() throws TauModelException {
        doTable("P");
    }
    @Test
    public void testTableS() throws TauModelException {
        doTable("S");
    }

    @Test
    public void testTablePcP() throws TauModelException {
        doTable("PcP");
    }
    
    @Test
    public void testTableScS() throws TauModelException {
        doTable("ScS");
    }
    
    @Test
    public void testTableScP() throws TauModelException {
        doTable("ScP");
    }
    
    @Test
    public void testTablePKIKP() throws TauModelException {
        doTable("PKIKP");
    }

    public void doTable(String phase) throws TauModelException {
        if (phase.equals("P")) {
            taup.setPhaseNames(new String[] {"p", "P", "Pdiff"});
        } else if (phase.equals("S")) {
            taup.setPhaseNames(new String[] {"s", "S", "Sdiff"});
        } else {
            taup.setPhaseNames(new String[] {phase});
        }
        for (List<TimeDist> atDepth : table.get(phase).values()) {
            taup.setSourceDepth(atDepth.get(0).getDepth());
            for (TimeDist timeDist : atDepth) {
                taup.calculate(timeDist.getDistDeg());
                List<Arrival> arrivals = taup.getArrivals();
                if (timeDist.getTime() > 0) {
                assertTrue(phase + " has arrivals for depth " + timeDist.getDepth() + " at dist " + timeDist.getDistDeg(),
                           arrivals.size() > 0);
                // assume first?
                assertEquals(phase + " time for depth " + timeDist.getDepth() + " at dist " + timeDist.getDistDeg(),
                             timeDist.getTime(),
                             arrivals.get(0).getTime(),
                             0.07f);
                assertEquals(phase + " rp for depth " + timeDist.getDepth() + " at dist " + timeDist.getDistDeg(),
                             timeDist.getP(),
                             arrivals.get(0).getRayParam() * Math.PI / 180,
                             0.11f);
                }
            }
        }
    }

    public void readTable(String filename, String phase) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass()
                .getClassLoader()
                .getResourceAsStream("edu/sc/seis/TauP/" + filename)));
        String line = in.readLine();
        float[] depths = parseLine(line);
        HashMap<Float, List<TimeDist>> phaseTable = table.get(phase);
        if (phaseTable == null) {
            phaseTable = new HashMap<Float, List<TimeDist>>();
            table.put(phase, phaseTable);
        }
        for (int i = 0; i < depths.length; i++) {
            phaseTable.put(depths[i], new ArrayList<TimeDist>());
        }
        while ((line = in.readLine()) != null) {
            String timeLine = line;
            float[] time = parseLine(line);
            line = in.readLine();
            float[] rayParam = parseLine(line);
            float dist = time[0];
            for (int i = 0; i < rayParam.length; i++) {
                try {
                time[i] = time[2 * i + 1] * 60 + time[2 * i + 2];
                }catch(Exception e) {
                    System.out.println(timeLine);
                    throw new RuntimeException(e);
                }
            }
            for (int i = 0; i < rayParam.length; i++) {
                TimeDist td = new TimeDist(rayParam[i], time[i], Arrival.DtoR*dist, depths[i]);
                phaseTable.get(depths[i]).add(td);
            }
        }
    }

    float[] parseLine(String line) {
        String[] valStr = line.split("\\s+");
        float[] val = new float[valStr.length];
        for (int i = 0; i < val.length; i++) {
            val[i] = Float.parseFloat(valStr[i]);
        }
        return val;
    }

    HashMap<String, HashMap<Float, List<TimeDist>>> table;

    TauP_Time taup;
}
