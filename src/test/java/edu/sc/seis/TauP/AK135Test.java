package edu.sc.seis.TauP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;
import static org.junit.jupiter.api.Assertions.*;


/** test data from http://rses.anu.edu.au/~brian/AK135tables.pdf
 */
public class AK135Test  {

    @BeforeEach
    protected void setUp() throws Exception {
        taup = new TimeTester("ak135");
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
        readTable("ak135_SKS.txt", "SKS");
        readTable("ak135_SKP.txt", "SKP");
        readTable("ak135_ScP.txt", "ScP");
        readTable("ak135_PKIKP.txt", "PKIKP");
        readTable("ak135_PKPab.txt", "PKPab");
        readTable("ak135_PKPbc.txt", "PKPbc");
        readTable("ak135_SKIKS.txt", "SKIKS");
    }

    public HashMap<String, HashMap<Float, List<TimeDist>>> getTable() {
        return table;
    }

    @Test
    public void testTableP() throws TauPException {
        doTable("P");
    }
    @Test
    public void testTableS() throws TauPException {
        doTable("S");
    }

    @Test
    public void testTablePcP() throws TauPException {
        doTable("PcP");
    }

    @Test
    public void testTableScS() throws TauPException {
        doTable("ScS");
    }

    @Test
    public void testTableSKS() throws TauPException {
        doTable("SKS");
    }

    @Test
    public void testTableSKP() throws TauPException {
        doTable("SKP");
    }

    @Test
    public void testTableSKIKS() throws TauPException {
        doTable("SKIKS");
    }

    @Test
    public void testTableScP() throws TauPException {
        doTable("ScP");
    }

    @Test
    public void testTablePKPab() throws TauPException {
        doTable("PKPab");
    }

    @Test
    public void testTablePKPbc() throws TauPException {
        doTable("PKPbc");
    }

    @Test
    public void testTablePKIKP() throws TauPException {
        doTable("PKIKP");
    }


    @Test
    public void zeroRPvsPublishedAK135() throws TauPException {
        TimeTester tool = new TimeTester("ak135");
        tool.setSourceDepth(0);
        VelocityModel vMod = tool.modelArgs.getTauModel().getVelocityModel();
        double[] layerTimes = Dist180Test.integrateVelocity(tool.modelArgs.getTauModel().getVelocityModel());

        double TIME_TOL_TAUP = 0.0051;

        double published_PKIKP = 20*60+ 12.53;
        tool.setPhaseNames(Collections.singletonList("PKIKP"));
        List<Arrival> arrivals = tool.calcAll(tool.getSeismicPhases(), List.of(DistanceRay.ofDegrees(180)));
        double taup_PKIKP = arrivals.get(0).getTime();
        double time_PKIKP = 0;
        for (int i = 0; i < layerTimes.length; i++) {
            time_PKIKP += 2 * layerTimes[i];
        }
        assertEquals(time_PKIKP, taup_PKIKP, TIME_TOL_TAUP, "PKIKP delta: " + (time_PKIKP - taup_PKIKP));
        // published PKIKP for ak135 differs from zero ray param calc by -0.049
        assertEquals(time_PKIKP, published_PKIKP, TIME_TOL, "PKIKP delta: " + (time_PKIKP - published_PKIKP));


        // PcP
        int cmb = vMod.layerNumberBelow(vMod.getCmbDepth());
        double time_PcP = 0;
        for (int i = 0; i < cmb; i++) {
            time_PcP += 2 * layerTimes[i];
        }

        double TIME_TOL_TAUP_PCP = 0.002;
        double TIME_TOL_PCP = 0.02;
        tool.setPhaseNames(Collections.singletonList("PcP"));
        List<Arrival> PcP_arrivals = tool.calcAll(tool.getSeismicPhases(), List.of(DistanceRay.ofDegrees(0)));
        Arrival PcP = PcP_arrivals.get(0);
        double  published_AK135_PcP = 8*60+ 31.69;
        assertEquals(time_PcP, PcP.getTime(), TIME_TOL_TAUP_PCP,
                "ak135 PcP zerocalc: "+time_PcP+" taup: "+ PcP.getTime()+" delta:"+(time_PcP- PcP.getTime()));
        // published PcP differs from zero ray param calc by -0.017
        assertEquals(time_PcP, published_AK135_PcP, TIME_TOL_PCP,
                "ak135 PcP published: "+published_AK135_PcP+" zerocalc: "+ time_PcP+" delta: "+(time_PcP- published_AK135_PcP));


        // PKiKP
        int iocb = vMod.layerNumberBelow(vMod.getIocbDepth());
        double time_PKiKP = 0;
        for (int i = 0; i < iocb; i++) {
            time_PKiKP += 2 * layerTimes[i];
        }

        tool.setPhaseNames(Collections.singletonList("PKiKP"));
        List<Arrival> PKiKP_arrivals = tool.calcAll(tool.getSeismicPhases(), List.of(DistanceRay.ofDegrees(0)));
        Arrival PKiKP = PKiKP_arrivals.get(0);
        double  published_AK135_PKiKP = 16*60+ 34.82;
        assertEquals(time_PKiKP, PKiKP.getTime(), TIME_TOL_TAUP,
                "ak135 PKiKP zerocalc: "+time_PKiKP+" taup: "+ PKiKP.getTime()+" delta:"+(time_PKiKP- PKiKP.getTime()));
        // published PKiKP differs from zero ray param calc by -0.041
        assertEquals(time_PKiKP, published_AK135_PKiKP, TIME_TOL,
                "ak135 PKiKP published: "+published_AK135_PKiKP+" zerocalc: "+ time_PKiKP+" delta: "+(time_PKiKP- published_AK135_PKiKP));
    }

    // error for PKPbc	50.0 km	0.055	154.00 deg
    // SKS	100.0 km	0.055	140.00 deg
    public static float TIME_TOL = 0.056f;  // seconds, roughly error for PKIKP at 130 deg with h=0, 1151.6700 vs 1151.6197
    public static float RAY_PARAM_TOL = 0.14f; // seconds per degree


    public static void main(String[] args) throws Exception {
        AK135Test ak135Test = new AK135Test();
        ak135Test.setUp();
        ak135Test.createAK135ErrorPlots();

    }

    public void createAK135ErrorPlots() throws IOException, TauPException {

        File topdir = new File("build");
        if ( ! topdir.isDirectory()) {topdir.mkdirs(); }
        File dir = new File(topdir, "ak135Compare");
        if ( ! dir.isDirectory()) {dir.mkdir(); }

        PrintStream indexOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(dir, "index.html"))));
        indexOut.println("<html>");
        indexOut.println("<body>");
        indexOut.println("<h3>Comparison with Published AK135 Times</h3>");
        indexOut.println("<table>");
        indexOut.println("<tr><th>Phase</th><th>Depth</th><th>Max Error (s)</th><th>at Deg</th><th>Error Plot</th></tr>");

        List<String> phaseList = List.of("P", "PcP", "PKPab", "PKPbc", "PKIKP", "S", "ScP", "ScS", "SKS", "SKP", "SKIKS");
        //List<String> phaseList = List.of("P");
        double totalMaxError = 0;
        for (String phase : phaseList) {
            Set<Float> depthSet = table.get(phase).keySet();
            List<Float> depthList = new ArrayList<>(depthSet);
            java.util.Collections.sort(depthList);
            //List<Float> depthList = List.of(100f);

            List<Double> all_err_deg = doErrorPlot(phase, depthList, dir);
            indexOut.println("<tr><td>"+phase+"</td><td>all"+"</td><td>"
                    +Outputs.formatRayParam(all_err_deg.get(0))+"</td><td>" +Outputs.formatDistance(all_err_deg.get(1))
                    +"</td><td><a target=\"_blank\" href=\"ak135_"+phase+"_all"+".html"+"\">compare</a></td></tr>");

            for (Float sourceDepth : depthList) {
                List<Double> err_deg = doErrorPlot(phase, List.of(sourceDepth), dir);
                double maxTimeError = err_deg.get(0);
                double atDeg = err_deg.get(1);
                indexOut.println("<tr><td>"+phase+"</td><td>"+sourceDepth+"</td><td>"
                        +Outputs.formatRayParam(maxTimeError)+"</td><td>" +Outputs.formatDistance(atDeg)
                        +"</td><td><a target=\"_blank\" href=\"ak135_"+phase+"_"+sourceDepth+".html"+"\">compare</a></td></tr>");
                if (maxTimeError > totalMaxError) {
                    totalMaxError = maxTimeError;
                }
            }
        }
        indexOut.println("</table>");
        indexOut.println("<h3>Max Error: "+totalMaxError+"</h3>");
        indexOut.println("</body>");
        indexOut.println("</html>");
        indexOut.close();
    }

    public List<Double> doErrorPlot(String phase, List<Float> sourceDepthList, File dir) throws TauPException, IOException {
        setupForPhase(phase);
        String phaseTitle = String.join(",", taup.phaseNameList);
        double maxTimeError = 0;
        double maxErrorAtDeg = 0;
        List<XYPlottingData> xyData = new ArrayList<>();
        for (Float sourceDepth : sourceDepthList){
            taup.setSourceDepth(sourceDepth);
            List<TimeDist> tdList = table.get(phase).get(sourceDepth);
            if ((phase.equals("PKPbc") && sourceDepth >= 500)
                    || (phase.equals("SKS") && (sourceDepth == 700 || sourceDepth == 500))) {
                // taup doesnt have 2 PKP arrivals at 155, but published ak135 does?
                tdList = tdList.subList(0, tdList.size() - 1);
            }
            List<TimeDist> pubPlotTDList = tdList;
            if (phase.equals("S")) {
                List<TimeDist> zeroToFifty = tdList.subList(0, 51);
                //pubPlotTDList = zeroToFifty;
            }
            double[] rayParam = createPlotData(pubPlotTDList, AxisType.rayparamrad);
            double[] pubtime = createPlotData(pubPlotTDList, AxisType.time);
            double[] pubdist = createPlotData(pubPlotTDList, AxisType.degree);
            double[] redtime = new double[pubtime.length];
            double redVel = 14; // sec/deg
            for (int i = 0; i < redtime.length; i++) {
                redtime[i] = pubtime[i]-pubdist[i]*redVel;
            }
            XYSegment seg = new XYSegment(pubdist, redtime);
            XYPlottingData publishedxyPlot = new XYPlottingData(List.of(seg), AxisType.rayparamrad.name(), AxisType.degree.name(), phase, new ArrayList<>());
            //xyData.add(xyPlot);

            // plot raw published values
            XYPlotOutput xyPlotOutput = new XYPlotOutput(List.of(publishedxyPlot) , taup.modelArgs);
            xyPlotOutput.setTitle("Published AK135 for "+phaseTitle+" at "+sourceDepth+" km, red="+redVel+" s/deg");
            String filename = "pub_ak135_"+phase+"_"+sourceDepthList.get(0)+".html";
            PrintWriter writer = new PrintWriter(new FileWriter(new File(dir, filename)));
            xyPlotOutput.printAsHtml(writer, "pub ak135", new ArrayList<>(), "", true);
            writer.close();

            List<TimeDist> taupTDList = new ArrayList<>();
            List<SeismicPhase> phaseList = taup.getSeismicPhases();
            for (TimeDist td : tdList) {
                if (td.getTime() > 0) {
                    List<Arrival> arrivals = taup.calcAll(phaseList, List.of(DistanceRay.ofRadians(td.getDistRadian())));
                    // otherwise assume first?
                    if (arrivals.size() == 0) {
                        System.err.println("Missing arrival for " + phaseTitle + " at " + td.getDistDeg() + " at depth " + sourceDepth);
                        continue;
                    }
                    Arrival a = arrivals.get(0);
                    // unless PKPab/PKPbc between 143 and 155 deg
                    if (phase.equals("PKPab") && (td.getDistDeg() < 155 || (td.getDistDeg() == 155 && sourceDepth <= 300))) {
                        assertEquals(2, arrivals.size(), "expect 2 arrivals for " + phase
                                + " at " + td.getDistDeg() + " at depth " + sourceDepth);
                        a = arrivals.get(1);
                    }
                    double deltaTime = a.getTime() - td.getTime();
                    taupTDList.add(new TimeDist(a.getRayParam(), deltaTime, a.getDist()));
                    if (Math.abs(deltaTime) > maxTimeError) {
                        maxTimeError = Math.abs(deltaTime);
                        maxErrorAtDeg = td.getDistDeg();
                    }
                }
            }
            double[] tauprayParam = createPlotData(taupTDList, AxisType.rayparamrad);
            double[] taupdeg = createPlotData(taupTDList, AxisType.degree);
            double[] tauptime = createPlotData(taupTDList, AxisType.time);
            XYSegment taupseg = new XYSegment(taupdeg, tauptime);
            XYPlottingData taupxyPlot = new XYPlottingData(List.of(taupseg),
                    AxisType.rayparamrad.name(), AxisType.degree.name(),
                    "TauP-AK for " + phaseTitle+" "+sourceDepth, new ArrayList<>());
            xyData.add(taupxyPlot);
        }

        // plot delta time
        XYPlotOutput xyPlotOutput = new XYPlotOutput(xyData, taup.modelArgs);
        String sourceTitle = "";
        for (Float sourceDepth : sourceDepthList){
            sourceTitle+=","+sourceDepth;
        }
        sourceTitle = sourceTitle.substring(1);
        xyPlotOutput.setTitle("TauP - Published AK135 for "+phaseTitle+" at "+sourceTitle+" km");
        String filename;
        if (sourceDepthList.size()==1) {
            filename = "ak135_"+phase+"_"+sourceDepthList.get(0)+".html";
        } else {
            filename = "ak135_"+phase+"_all"+".html";
        }
        PrintWriter writer = new PrintWriter(new FileWriter(new File(dir, filename)));
        xyPlotOutput.printAsHtml(writer, "testak135", new ArrayList<>(), "", true);

        writer.close();
        PrintWriter jsonWriter = new PrintWriter(new FileWriter(new File(dir, filename.replace(".html", ".json"))));
        xyPlotOutput.printAsJSON(jsonWriter, 2);
        jsonWriter.close();

        return List.of(maxTimeError, maxErrorAtDeg);
    }

    double[] createPlotData(List<TimeDist> tdList, AxisType axisType) throws TauPException {
        double[] out = new double[tdList.size()];
        for (int i = 0; i < out.length; i++) {
            TimeDist td = tdList.get(i);
            switch (axisType) {
                case rayparamrad:
                    out[i] = td.getP();
                    break;
                case rayparamdeg:
                    out[i] = td.getP()/RtoD;
                    break;
                case degree:
                    out[i] = td.getDistDeg();
                    break;
                case radian:
                    out[i] = td.getDistRadian();
                    break;
                case time:
                    out[i] = td.getTime();
                    break;
                default:
                    throw new TauPException("Cannot create plot for "+axisType);
            }
        }
        return out;
    }


    public void setupForPhase(String phase) throws TauPException {
        if (phase.equals("P")) {
            taup.setPhaseNames(List.of("p", "P", "Pdiff"));
            assertEquals(3, taup.phaseNameList.size());
        } else if (phase.equals("PKPab") || phase.equals("PKPbc")) {
            taup.setPhaseNames(List.of("PKP"));
            assertEquals(1, taup.phaseNameList.size());
        } else if (phase.equals("S")) {
            taup.setPhaseNames(List.of("s", "S", "Sdiff"));
            assertEquals(3, taup.phaseNameList.size());
        } else if (phase.equals("SKP")) {
            taup.setPhaseNames(List.of("SKP", "SKIKP"));
            assertEquals(2, taup.phaseNameList.size());
        } else {
            taup.setPhaseNames(List.of(phase));
        }
    }

    boolean closeDeg(double degA, double degB) {
        return Math.abs(degA-degB)<.1;
    }

    public void doTable(String phase) throws TauPException {
        setupForPhase(phase);
        for (List<TimeDist> atDepth : table.get(phase).values()) {
            double sourceDepth = atDepth.get(0).getDepth();
            taup.setSourceDepth(sourceDepth);
            for (TimeDist timeDist : atDepth) {
                assertNotNull(timeDist);
                if (timeDist.getTime() > 0) {

                    if (phase.equals("SKIKS") && closeDeg(timeDist.getDistDeg(), 105) && sourceDepth >= 200) {
                        // taup doesnt have SKIKS arrivals, but published ak135 does. TauP thinks phase starts at 105.01
                        continue;
                    }
                    if (phase.equals("PKPbc") && closeDeg(timeDist.getDistDeg(),  155) && sourceDepth >= 500) {
                        // taup doesnt have 2 PKP arrivals, but published ak135 does?
                        continue;
                    }
                    if (phase.equals("SKS") && closeDeg(timeDist.getDistDeg(),  144) && sourceDepth >= 500) {
                        // published ak135 for 144 at 500 and 700 km looks like copy/paste from 143 deg
                        continue;
                    }
                    if (phase.equals("SKP") && closeDeg(timeDist.getDistDeg(),  110) && sourceDepth >= 300) {
                        // published ak135 for 144 at 500 and 700 km looks like copy/paste from 143 deg
                        continue;
                    }

                    List<Arrival> arrivals = taup.calcAll(taup.getSeismicPhases(), List.of(DistanceRay.ofDegrees(timeDist.getDistDeg())));
                    assertFalse(arrivals.isEmpty(), "got no arrivals for " + phase + " at deg=" + timeDist.getDistDeg()
                            + " depth=" + taup.modelArgs.getSourceDepths());

                    // otherwise assume first?
                    Arrival arrival = arrivals.get(0);
                    // unless PKPab/PKPbc between 143 and 155 deg
                    if (phase.equals("PKPab") &&
                            ((sourceDepth < 500 && timeDist.getDistDeg() <= 155)
                                    || (sourceDepth >= 500 && timeDist.getDistDeg() <= 154)) ) {
                        assertEquals(2, arrivals.size(), "expect 2 arrivals for "+phase
                                +" at "+timeDist.getDistDeg()+" at depth "+sourceDepth);
                        arrival = arrivals.get(1);
                    }
                    assertEquals(timeDist.getTime(),
                                 arrival.getTime(),
                            TIME_TOL,
                            timeDist.getDistDeg()+" at "+timeDist.getDepth()+" "+phase
                                    +" delta: "+(timeDist.getTime()-arrival.getTime()+" "+arrival));
                    assertEquals(timeDist.getP(),
                                 arrival.getRayParamDeg() ,
                                 RAY_PARAM_TOL,
                            timeDist.getDistDeg()+" at "+timeDist.getDepth()+" "+phase+" rp="+timeDist.getP()
                                    +" deltarp="+(timeDist.getP()-arrival.getRayParamDeg()));
                }
            }
        }
    }

    public void readTable(String filename, String phase) throws IOException, TauPException {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass()
                .getClassLoader()
                .getResourceAsStream("edu/sc/seis/TauP/" + filename)));
        String line = in.readLine().trim(); // phase line
        if ( ! ( line.startsWith(phase)
                || (phase.equals("PKIKP") && line.equals("PKPdf"))
                || (phase.equals("SKIKS") && line.equals("SKSdf")))) {
            throw new TauPException("AK135 file for wrong phase: "+line+" != "+phase);
        }
        line = in.readLine().trim(); // commentline Depth of source [km]
        line = in.readLine().substring(1).trim(); // depth list, first char is "delta"
        float[] depths = parseLine(line);
        line = in.readLine().trim(); // m s m s m s m s m s m s m s m s
        HashMap<Float, List<TimeDist>> phaseTable = table.computeIfAbsent(phase, k -> new HashMap<Float, List<TimeDist>>());
        for (int i = 0; i < depths.length; i++) {
            phaseTable.put(depths[i], new ArrayList<TimeDist>());
        }
        while ((line = in.readLine()) != null) {
            String timeLine = line.trim();
            float[] time = parseLine(line);
            line = in.readLine().trim();
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
                TimeDist td = new TimeDist(rayParam[i], time[i], SphericalCoords.DtoR*dist, depths[i]);
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

    TimeTester taup;
}
