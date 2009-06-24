package edu.sc.seis.TauP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

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
    }
    
    public HashMap<String, HashMap<Float, List<TimeDist>>> getTable() {
        return table;
    }

    public void tXXXestTableP() throws TauModelException {
        doTable("P");
    }
    public void tXXXestTableS() throws TauModelException {
        doTable("S");
    }

    public void tXXXestTablePcP() throws TauModelException {
        doTable("PcP");
    }
    public void tXXXestTableScS() throws TauModelException {
        doTable("ScS");
    }
    public void tXXXestTableScP() throws TauModelException {
        doTable("ScP");
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
            taup.depthCorrect(atDepth.get(0).depth);
            for (TimeDist timeDist : atDepth) {
                taup.calculate(timeDist.dist);
                Arrival[] arrivals = taup.getArrivals();
                if (timeDist.time > 0) {
                assertTrue(phase + " has arrivals for depth " + timeDist.depth + " at dist " + timeDist.dist,
                           arrivals.length > 0);
                // assume first?
                assertEquals(phase + " time for depth " + timeDist.depth + " at dist " + timeDist.dist,
                             timeDist.time,
                             arrivals[0].time,
                             0.03f);
                assertEquals(phase + " rp for depth " + timeDist.depth + " at dist " + timeDist.dist,
                             timeDist.p,
                             arrivals[0].rayParam * Math.PI / 180,
                             0.1f);
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
                TimeDist td = new TimeDist(rayParam[i], time[i], dist, depths[i]);
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
