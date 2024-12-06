package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.BuildVersion;
import edu.sc.seis.seisFile.sac.*;
import edu.sc.seis.seisFile.mseed3.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;
import java.net.URL;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class CmdLineOutputTest {

    String[] timeTestCmds = new String[] {"taup time -h 10 -p P --deg 35 --mod prem",
                                          "taup time -h 10 -p P --deg 35",
                                          "taup time -h 10 -p P --deg 35 --mod ak135",
                                          "taup time -h 10 -p ttall --deg 35 --mod prem",
                                          "taup time -h 10 -p ttall --deg 35",
                                          "taup time -h 10 -p ttall --deg 35 --mod ak135",
                                          "taup time -h 10 -p ttall --deg 35 --mod ak135 --json",
                                          "taup time -h 10 -p ttall --deg 145 --mod ak135 --rel P",
                                          "taup time -h 10 -p ttall --deg 145 --mod ak135 --rel P,PKP",
            "taup time -h 10 -p ttall --deg 145 --mod ak135 --stadepth 200",
            "taup time -h 10,100 -p P --deg 35",
            "taup time -h 10 --stadepth 1,10 -p P --deg 3",
                                            };

    String[] pierceTestCmds = new String[] {"taup pierce -h 10 -p P --deg 35 --mod prem",
                                            "taup pierce -h 10 -p P --deg 35",
                                            "taup pierce -h 10 -p P --deg 35 --mod ak135",
                                            "taup pierce --mod prem -h 600 --deg 45 -p PKiKP --pierce 5049.5",
                                            "taup pierce -h 0 -p Pn --deg 6",
                                            "taup pierce -h 0 -p Pdiff --deg 120",
            "taup pierce -h 10 -p ttall --deg 145 --mod ak135 --stadepth 200",
                                          };

    String[] pathTestCmds = new String[] {"taup path -o stdout -h 10 -p P --deg 35 --mod prem",
                                          "taup path -o stdout -h 10 -p P --deg 35",
            "taup path -o stdout -h 10 -p Pdiff --deg 135",
            "taup path -o stdout -h 10 -p 2kmps --deg 35",
            "taup path -o stdout -h 10 -p Pn --deg 10",
            "taup path -o stdout -h 10 -p PnPn --deg 10",
            "taup path -o stdout -h 10 -p PdiffPdiff --deg 135",
                                          "taup path -o stdout -h 10 -p P --deg 35 --svg",
                                          "taup path -o stdout -h 10 -p P --deg 35 --mod ak135",

            "taup path -o stdout -h 10 -p Pdiff --deg 135 --svg",
            "taup path -o stdout -h 10 -p 2kmps --deg 35 --svg",
            "taup path -o stdout -h 10 -p Pn --deg 10 --svg",
            "taup path -o stdout -h 10 -p PnPn --deg 10 --svg",
            "taup path -o stdout -h 10 -p PdiffPdiff --deg 135 --svg",
            "taup path -o stdout -h 10 -p ttall --deg 35 --svg",
            "taup path -o stdout --mod ak135 --svg -h 10 -p P,S,PedOP --scatter 200 -5 --deg 40"
    };

    String[] phaseDescribeTestCmds = new String[] {
            "taup phase -p Pdiff",
            "taup phase -p P410diff",
            "taup phase -p Pv410p,PV410p",
            "taup phase -p P410s,P410S",
            "taup phase -p Ped410S,Pedv410s,PedV410s",
            "taup phase -p PKviKP",
            "taup phase -p PKv5153KP",
            "taup phase -p PKv5153.9KP",
            "taup phase -p PK5153.9diffP",
            "taup phase -p PKP410S",
    };

    String[] curveTestCmds = new String[] {
            "taup curve -o stdout -h 10 -p P --mod prem",
            "taup curve -o stdout -h 10 -p P",
            "taup curve -o stdout -h 10 -p P --mod ak135",
            "taup curve -o stdout -h 10 -p P --mod ak135 --redkm 8",
            // curve labels are random position in --svg, so diff breaks
            // "taup curve -o stdout -h 10 -p P --svg",

            "taup curve -o stdout -h 10 -p P,2kmps --mod prem --svg",
    };

    String[] wavefrontTestCmds = new String[] {
            "taup wavefront -o stdout --mod ak135 --svg -h 100 -p P,S,PKIKP --timestep 100",
            "taup wavefront -o stdout --mod ak135 --svg -h 10 -p P,S,PedOP --scatter 200 -5 --timestep 25 --color wavetype"
    };

    String[] velplotTestCmds = new String[] {
            "taup velplot -o stdout --mod ak135 --svg",
            "taup velplot -o stdout --mod ak135 --mod ak135favg --mod ak135fcont --mod prem --svg",
            "taup velplot -o stdout --mod ak135 --svg -x velocity_density",
            "taup velplot -o stdout --mod ak135 --svg -x slownessrad",
            "taup velplot -o stdout --mod ak135 --yminmax 0 700 --svg",
            "taup velplot -o stdout --mod ak135 --csv",
            "taup velplot -o stdout --mod ak135 --text",
            "taup velplot -o stdout --mod ak135 --gmt",
            "taup velplot -o stdout --mod ak135 --json",
            "taup velplot -o stdout --mod ak135 --svg ",
            "taup velplot -o stdout --mod ak135 --svg -x poisson",
    };

    String[] reflTransPlotTestCmds = new String[] {
            "taup refltrans -o stdout --mod ak135 --depth 35 --svg",
            "taup refltrans -o stdout --abs --anglestep 1 --svg --fsrf --legend --model ak135fcont --pwave --swave -x rayparam",
            "taup refltrans -o stdout --abs --anglestep 1 --svg --fsrf --legend --model ak135favg --pwave --swave -x rayparam"
    };


    String[] helpTestCmds = new String[] {"taup --help",
                                          "taup time --help",
                                          "taup pierce --help",
                                          "taup path --help",
                                          "taup phase --help",
                                          "taup curve --help",
                                          "taup wavefront --help",
                                          "taup table --help",
                                          "taup velmerge --help",
                                          "taup velplot --help",  // bug in picocli gives randome order to vmod args
                                          "taup create --help",
                                          "taup refltrans --help",
                                          "taup setsac --help",
                                          "taup setms3 --help",
                                          "taup version --help",
                                          "taup spikes --help",
    };

    String[] jsonTestCmds = new String[] {
            "taup time -h 10 -p P --deg 35 --json",
    };

    String versionCmd = "taup --version";

    String[] docCmds = new String[] {
            "taup time --mod prem -h 200 -p S,P --deg 57.4",
            "taup find --max 2 -h 100 --exclude 210",
            "taup find --max 2 --deg 35 -h 100 --time 400 420 --exclude 210",
            "taup pierce --mod prem -h 200 -p S,P --deg 57.4",
            "taup pierce --turn --mod prem -h 200 -p S,P --deg 57.4",
            "taup pierce --mod prem -h 200 -p S --sta 12 34.2 --evt -28 122 --geodetic --pierce 2591 --nodiscon",
            "taup path --mod iasp91 -h 550 --deg 74 -p S,ScS,sS,sScS --gmt",
            "taup path --mod iasp91 -h 550 --deg 74 -p S,ScS,sS,sScS --svg",
            "taup phase --mod prem -h 200 -p PKiKP",
            "taup wavefront --mod iasp91 -h 550 -p s,S,ScS,sS,sScS --gmt",
            "taup wavefront --mod iasp91 -h 550 -p s,S,ScS,sS,sScS --color wavetype --svg",
            "taup curve --mod prem -h 500 -p s,S,ScS,Sdiff --gmt",
            "taup curve --mod prem -h 500 -p s,S,ScS,Sdiff --legend --svg",
            "taup curve -x rayparamdeg -y tau --legend --svg",
            "taup velplot --mod ak135fcont --mod prem --svg",
            "taup velplot --mod ak135fcont -x poisson --svg",
            "taup velplot --nameddiscon --mod iasp91",
            "taup refltrans --mod ak135 --depth 35 --legend --svg",
            "taup refltrans --mod ak135 --depth 35 --legend --energyflux --svg"
    };

    String[] setInFileCmds = new String[] {

            "taup setsac -p P,S my_earthquake.sac",
            "taup setms3 -p P,S my_earthquake.ms3",
            "taup time --quakeml my_midatlantic.qml --staxml my_stations.staml --geodetic -p P,S"
    };

    /** 
     * regenerating the cmd line output test resources.
     * new text files will be in cmdLineTest in cwd
     *
     * @throws Exception
     */
    public void regenSavedOutput() throws Exception {
        List<String> allList = new ArrayList<String>();
        allList.addAll(Arrays.asList(helpTestCmds));
        allList.addAll(Arrays.asList(timeTestCmds));
        allList.addAll(Arrays.asList(pierceTestCmds));
        allList.addAll(Arrays.asList(pathTestCmds));
        allList.addAll(Arrays.asList(curveTestCmds));
        allList.addAll(Arrays.asList(wavefrontTestCmds));
        allList.addAll(Arrays.asList(velplotTestCmds));
        allList.addAll(Arrays.asList(reflTransPlotTestCmds));
        allList.addAll(Arrays.asList(phaseDescribeTestCmds));
        allList.addAll(Arrays.asList(jsonTestCmds));
        for (String cmd : allList) {
            System.err.println(cmd);
            saveTestOutputToFile(cmd);
        }
        // java verison only for html view
        allList.add("java --version");
        saveJavaVersionToFile(testOutputDir);
        viewSavedOutputAsHTML(allList, testOutputDir, "Command Line Test Cases");
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(testOutputDir, "all_commands.txt")));
        for (String cmd : allList) {
            out.write(cmd);
            out.newLine();
        }
        out.close();
        usageToDocSrc();
    }

    public void viewSavedOutputAsHTML(List<String> allList, File outputDir, String title) throws FileNotFoundException {
        String html = "<!DOCTYPE html>\n"
                +"<html>\n"
                +"<head>\n"
                +"  <style>\n"
                +"  body {\n"
                +"    background-color: gainsboro;\n"
                +"  }"
                +"  #testoutput {\n"
                +"    background-color: white;\n"
                +"  }"
                +"  </style>\n"
                +"</head>\n"
        +"<body>\n"
        +"<h3>"+title+"</h3>\n"
        +"  <select name=\"cmdlinetests\" id=\"cmd-select\">\n";
        for (String cmd : allList) {
            html += "  <option>"+cmd+"</option>\n";
        }
        html += "  </select>\n"
                +"  <button id=\"prev\">Prev</button>\n"
                +"  <button id=\"next\">Next</button>\n"
                +"  <h5 id=\"cmdvalue\">\n"
                +"  </h5>\n"
                +"  <div id=\"testoutput\">\n"
                +"  </div>\n";
        String javascript = "\n"
                +"function fileizeCmd(cmd) {\n" +
                "        cmd = cmd.replaceAll(\",\", \"_\");\n" +
                "        return cmd.replaceAll(\" \", \"_\");\n" +
                "}\n"
                +"const selectEl = document.querySelector(\"select\");\n"
                +"const outputEl = document.querySelector(\"#testoutput\");\n"
                +"const cmdEl = document.querySelector(\"#cmdvalue\");\n"
                +"function doCmd(cmd) {\n"
                +"  while (outputEl.firstChild) {\n"
                +"    outputEl.removeChild(outputEl.lastChild);\n"
                +"  }\n"
                +"  cmdEl.textContent = cmd;\n"
                +"  console.log(cmd);\n"
                +"  fetch(fileizeCmd(cmd), {cache: \"no-cache\"}).then(resp => {\n"
                +"    if (!resp.ok) {\n"
                +"      throw new Error(`HTTP error! Status: ${resp.status}`);\n"
                +"    }\n"
                +"    return resp.text();\n"
                +"  }).then(textResp => {\n"
                +"    if (cmd.includes(\"--svg\")) {\n"
                +"      outputEl.innerHTML = textResp;\n"
                +"    } else {\n"
                +"      const embEl = document.createElement(\"pre\");\n"
                +"      embEl.textContent = textResp;\n"
                +"      outputEl.appendChild(embEl);\n"
                +"    }\n"
                +"  }).catch(e => {\n"
                +"    outputEl.textContent = e;\n"
                +"    console.warn(e);\n"
                +"  });\n"
                +"}\n"
                +"selectEl.addEventListener(\"change\", e => {\n"
                +"  const cmd = e.target.value;\n"
                +"  doCmd(cmd);\n"
                +"});\n"
                +"doCmd(selectEl.options[selectEl.selectedIndex].text);\n"
                +"document.querySelector(\"#prev\").addEventListener(\"click\", e => {\n"
                +"  let idx = selectEl.selectedIndex;\n"
                +"  if (idx > 0) {\n"
                +"    idx = idx-1;\n"
                +"  } else {\n"
                +"    idx = selectEl.options.length-1;\n"
                +"  }\n"
                +"  selectEl.selectedIndex = idx;\n"
                +"  doCmd(selectEl.options[selectEl.selectedIndex].text);\n"
                +"});\n"
                +"document.querySelector(\"#next\").addEventListener(\"click\", e => {\n"
                +"  let idx = selectEl.selectedIndex;\n"
                +"  if (idx < selectEl.options.length-1) {\n"
                +"    idx = idx+1;\n"
                +"  } else {\n"
                +"    idx = 0;\n"
                +"  }\n"
                +"  selectEl.selectedIndex = idx;\n"
                +"  doCmd(selectEl.options[selectEl.selectedIndex].text);\n"
                +"});\n"

                +"\n"
                +"\n";

        html += "<script>\n"
                +javascript
                +"\n</script>\n";
        html += "</body>\n"
                +"</html>\n";
        PrintStream indexOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(outputDir, "index.html"))));
        indexOut.println(html);
        indexOut.close();

    }

    public void regenExampleOutput() throws Exception {
        // create dummy sac and ms3 files for setsac and setms3 examples
        SacHeader sacHeader = SacHeader.createEmptyEvenSampledTimeSeriesHeader();
        sacHeader.setNzTime(Instant.parse("2024-08-30T17:47:41Z"));
        sacHeader.setSourceId(FDSNSourceId.createUnknown(sacHeader.getDelta()));
        sacHeader.setEvla(52);
        sacHeader.setEvlo(-33);
        sacHeader.setEvdp(10*1000);
        sacHeader.setMag(5.2f);
        sacHeader.setStla(34.28f);
        sacHeader.setStlo(-81.25f);
        sacHeader.setStel(0);
        sacHeader.setStdp(0);
        sacHeader.setO(0);
        sacHeader.setB(0);

        SacTimeSeries sac = new SacTimeSeries(sacHeader, new float[] { 0, 0, 1, 2, 3, 3, 2, 1, -1, -2, -3 -2, 1, 0});
        sac.write(new File(docOutputDir, "my_earthquake.sac"));
        MSeed3Record ms3 = MSeed3Convert.convertSacTo3(sac);
        OutputStream dos = new BufferedOutputStream(new FileOutputStream(new File(docOutputDir, "my_earthquake.ms3")));
        try {
            ms3.write(dos);
        } finally {
            dos.close();
        }

        for (String cmd : setInFileCmds) {
            String inDirCmd = cmd.replaceAll(" my_", " "+docOutputDir+"/my_");
            System.err.println(inDirCmd);
            String filename = fileizeCmd(cmd);
            saveTestOutputToFile( inDirCmd, docOutputDir, filename);
            PrintStream cmdOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(docOutputDir, filename+".cmd"))));
            cmdOut.println(cmd);
            cmdOut.close();
        }


        for (String cmd : docCmds) {
            System.err.println(cmd);
            String filename = fileizeCmd(cmd); // without -o stdout
            String cmdStdOut = cmd+" -o stdout";
            saveTestOutputToFile( cmdStdOut, docOutputDir, filename);
            PrintStream cmdOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(docOutputDir, filename+".cmd"))));
            cmdOut.println(cmd);
            cmdOut.close();
        }
        viewSavedOutputAsHTML(List.of(docCmds), docOutputDir, "Command Line Doc Examples");
    }

    /**
     * reproduce figures from books for comparison
     * new text files will be in cmdLineTest/refltranCompare in cwd
     *
     * @throws Exception
     */
    public void regenReflTranCompareFigures() throws Exception {

        // not actual tests, but allow visual compare with figures in
        // Foundations of Modern Global Seismology
        HashMap<String, String> fmgsFigureTestCmds = new HashMap<>();
        HashMap<String, String> figureTitles = new HashMap<>();
        HashMap<String, String> figureCompare = new HashMap<>();
        fmgsFigureTestCmds.put("FMGS_fig_13_12a.svg",
                "taup refltrans -o stdout --abs --pwave --layer 8.0 4.6 3.3 6.4 3.7 2.8 -x rayparam --svg");
        figureTitles.put("FMGS_fig_13_12a.svg", "FMGS, fig 13.12a, solid-solid P");
        figureCompare.put("FMGS_fig_13_12a.svg", "FMGS_fig_13_12a.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_12b.svg",
                "taup refltrans -o stdout --abs --swave --layer 8.0 4.6 3.3 6.4 3.7 2.8 -x rayparam --yminmax -0.05 1.3 --svg");
        figureTitles.put("FMGS_fig_13_12b.svg", "FMGS, fig 13.12b, solid-solid S");
        figureCompare.put("FMGS_fig_13_12b.svg", "FMGS_fig_13_12b.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_13a.svg",
                "taup refltrans -o stdout --abs --pwave --layer 6.4 3.7 2.8 8.0 4.6 3.3 -x rayparam --yminmax 0 2 --svg");
        figureTitles.put("FMGS_fig_13_13a.svg", "FMGS, fig 13.13a, solid-solid P");
        figureCompare.put("FMGS_fig_13_13a.svg", "FMGS_fig_13_13a.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_13b.svg",
                "taup refltrans -o stdout --abs --swave --layer 6.4 3.7 2.8 8.0 4.6 3.3 -x rayparam --yminmax 0 2 --svg");
        figureTitles.put("FMGS_fig_13_13b.svg", "FMGS, fig 13.13b, solid-solid S");
        figureCompare.put("FMGS_fig_13_13b.svg", "FMGS_fig_13_13b.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_14a.svg",
                "taup refltrans -o stdout --abs --pwave --layer 5.8 3.35 2.5 0.3 0 1.2 -x rayparam --yminmax 0 2.5 --svg");
        figureTitles.put("FMGS_fig_13_14a.svg", "FMGS, fig 13.14a, solid-air P");
        figureCompare.put("FMGS_fig_13_14a.svg", "FMGS_fig_13_14a.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_14b.svg",
                "taup refltrans -o stdout --abs --swave --layer 5.8 3.35 2.5 0.3 0 1.2 -x rayparam -y Rss Rsp Tsp --svg");
        figureTitles.put("FMGS_fig_13_14b.svg", "FMGS, fig 13.14b, solid-air P");
        figureCompare.put("FMGS_fig_13_14b.svg", "FMGS_fig_13_14b.png");
        fmgsFigureTestCmds.put("FMGS_fig_13_15.svg",
                "taup refltrans -o stdout --abs --pwave  --swave --layer 5.8 3.35 2.5 0 0 0 -x rayparam -y Rpp Rps Rsp Rss --yminmax 0 3.3 --svg");
        figureTitles.put("FMGS_fig_13_15.svg", "FMGS, fig 13.15, free surface");
        figureCompare.put("FMGS_fig_13_15.svg", "FMGS_fig_13_15.png");
        fmgsFigureTestCmds.put("AR_fig_5_06.svg",
                "taup refltrans -o stdout --pwave --swave --layer 5 3 2.5 0 0 0 -x rayparam -y Rpp Rps Rsp Rss --svg");
        figureTitles.put("AR_fig_5_06.svg", "Aki and Richards, fig 5.6");
        figureCompare.put("AR_fig_5_06.svg", "AR_fig_5_6.pdf");
        fmgsFigureTestCmds.put("AR_fig_5_10.svg",
                "taup refltrans -o stdout --abs --swave --layer 5 3 2.5 0 0 0 -x rayparam  -y Rsp Rss --svg");
        figureTitles.put("AR_fig_5_10.svg", "Aki and Richards, fig 5.10");
        figureCompare.put("AR_fig_5_10.svg", "AR_fig_5_10.pdf");
        fmgsFigureTestCmds.put("Shearer_fig_6_5.svg",
                "taup refltrans -o stdout  --shwave --mod prem --depth 24.4 --down --svg");
        figureTitles.put("Shearer_fig_6_5.svg", "Shearer, fig 6.5");
        figureCompare.put("Shearer_fig_6_5.svg", "Shearer_fig_6_5.pdf");

        fmgsFigureTestCmds.put("FMGS_13_16.svg",
                "taup refltrans --abs --anglestep 1 --down --svg --fsrf --layer 5.8 3.35 2.9 0 0 0 --legend --pwave --swave -x rayparam -o stdout");
        figureTitles.put("FMGS_13_16.svg", "FMGS, fig 13.16, free surface RF");
        figureCompare.put("FMGS_13_16.svg", "FMGS_fig_13_16.png");


        File topdir = new File("build/cmdLineTest");
        if ( ! topdir.isDirectory()) {topdir.mkdirs(); }
        File dir = new File(topdir, "refltranCompare");
        if ( ! dir.isDirectory()) {dir.mkdir(); }

        PrintStream indexOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(dir, "index.html"))));
        indexOut.println("<html>");
        indexOut.println("<body>");
        indexOut.println("<h3>Comparison of Reflection and Transmission Coefficients</h3>");
        indexOut.println("<table>");
        indexOut.println("<tr><th>TauP SVG</th><th>Command Line</th><th>Reference Image</th></tr>");
        List<String> sortedKeys = new ArrayList<String>();
        sortedKeys.addAll(fmgsFigureTestCmds.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            String cmd = fmgsFigureTestCmds.get(key);
            System.err.println(cmd);
            saveTestOutputToFile( cmd, dir, key);
            String compare = figureCompare.containsKey(key) ? figureCompare.get(key) : "";
            indexOut.println("<tr><td><a href=\""+key+"\">"+figureTitles.get(key)+"</a></td><td><code>"
                    +cmd.replace("-o stdout", "")+"</code></td><td><a target=\"_blank\" href=\""+compare+"\">compare</a></td></tr>");
        }
        indexOut.println("</table>");
        indexOut.println("</body>");
        indexOut.println("</html>");
        indexOut.close();
    }
    /** 
     * regenerating the cmd line output test resources.
     * new text files will be in cmdLineTest in cwd.
     * This one just does a single test for when adding new output test.
     *
     * @throws Exception
     */
    public void regenSavedOutputSingle() throws Exception {
        saveTestOutputToFile(helpTestCmds[0]);
    }
    
    /**
     * version test needs to be separate because act of committing an update to the version cmd line output
     * changes the git version making the test fail.
     * 
     * @throws Exception
     */
    @Test
    public void testVersion() throws Exception {
        String currentLine = runCmd(versionCmd);
        BuildVersion ver;
        // should only be one line
        assertEquals(1, currentLine.split("\\n").length);
        assertTrue(currentLine.contains(BuildVersion.getGroup()));
        assertTrue(currentLine.contains(BuildVersion.getName()));
        assertTrue(currentLine.contains(BuildVersion.getVersion()));
    }

    @Test
    @Disabled
    public void testTauPHelp() throws Exception {
        runTests(helpTestCmds);
    }

    @Test
    public void testTauPTime() throws Exception {
        runTests(timeTestCmds);
    }

    @Test
    public void testTauPPierce() throws Exception {
        runTests(pierceTestCmds);
    }

    @Test
    public void testTauPPath() throws Exception {
        runTests(pathTestCmds);
    }

    @Test
    public void testTauPPhaseDescribe() throws Exception {
        runTests(phaseDescribeTestCmds);
    }

    @Test
    public void testTauPCurve() throws Exception {
        runTests(curveTestCmds);
    }

    @Test
    public void testTauPWavefront() throws Exception {
        runTests(wavefrontTestCmds);
    }

    @Test
    public void testTauPVelplot() throws Exception {
        runTests(velplotTestCmds);
    }

    @Test
    public void testTauPReflTransplot() throws Exception {
        runTests(reflTransPlotTestCmds);
    }


    @Test
    public void testTauPJSON() throws Exception {
        runJsonTests(jsonTestCmds);
    }
    @Test
    @Disabled
    public void testTauPTable() throws Exception {
        // this one takes a lot of memory
       runTests(new String[] {"taup table -p ttall -generic"});
    }

    public void runTests(String[] cmds) throws Exception {
        for (int i = 0; i < cmds.length; i++) {
            testCmd(cmds[i]);
        }
    }

    public void runJsonTests(String[] cmds) throws Exception {
        for (int i = 0; i < cmds.length; i++) {
            testJsonCmd(cmds[i]);
        }
    }

    public String runCmd(String cmd) throws Exception {
        StringWriter sw = new StringWriter();
        runCmdWithWriter(cmd, new PrintWriter(sw));
        return sw.toString();
    }

    public void runCmdWithWriter(String cmd, PrintWriter writer) throws Exception {
        try {
            String[] s = cmd.split(" +");
            String tool = s[0];
            if (!tool.equalsIgnoreCase("taup")) {
                throw new Exception("Unknown first word of command: " + tool + ", should be taup");
            }
            String[] cmdArgs = new String[s.length - 1];
            System.arraycopy(s, 1, cmdArgs, 0, cmdArgs.length);
            System.err.println(cmd);

            ToolRun toolRun = new ToolRun();
            CommandLine cmdLine = new CommandLine(toolRun);

            cmdLine.setOut(writer);

            int exitCode = cmdLine.execute(cmdArgs);
            assertEquals(0, exitCode, "exit code=" + exitCode + "  " + cmd);
            writer.flush();
        } catch (Exception e) {
            throw new Exception("problem with cmd: "+cmd, e);
        }
    }

    public void testCmd(String cmd) throws Exception {
        String outContent = runCmd(cmd);
        assertNotNull(outContent);
        assertNotEquals(0, outContent.length());
        BufferedReader prior = getPriorOutput(cmd);
        BufferedReader current = new BufferedReader(new StringReader(outContent));
        int lineNum = 1;
        String priorLine;
        String currentLine;
        assertTrue( current.ready() , "Current output is empty for "+cmd);
        while (prior.ready() && current.ready()) {
            priorLine = prior.readLine();
            currentLine = current.readLine();
            String priorTrimmed = priorLine.trim();
            if ((priorTrimmed.startsWith("# version") || priorTrimmed.startsWith("<version>"))) {
                // don't check version lines as those different even if no actual change
                continue;
            }
            assertEquals(priorLine, currentLine, cmd + " line " + lineNum);
            lineNum++;
        }
        while (prior.ready()) {
            priorLine = prior.readLine();
            assertEquals(0, priorLine.trim().length(), "Prior has extra lines: " + priorLine);
        }
        while (current.ready()) {
            currentLine = current.readLine();
            if (currentLine == null) {break;}
            assertEquals(0, currentLine.trim().length(), "Current has extra lines: " + currentLine);
        }
        System.err.println("Done with " + cmd);
    }

    public void testJsonCmd(String cmd) throws Exception {
        String outContent = runCmd(cmd);
        assertNotNull(outContent);
        assertNotEquals(0, outContent.length());
        BufferedReader prior = getPriorOutput(cmd);
        BufferedReader current = new BufferedReader(new StringReader(outContent));
        JSONTokener jsonIn = new JSONTokener(prior);
        JSONObject priorJson = new JSONObject(jsonIn);
        JSONTokener currentIn = new JSONTokener(current);
        JSONObject currentJson = new JSONObject(currentIn);
        assertTrue(priorJson.similar(currentJson), currentJson.toString(2));
    }

    /**
     * test loading prior results text file from test resources. Kind of a meta-test... :)
     * @throws Exception
     */
    @Test
    public void loadTest() throws Exception {
        BufferedReader s = getPriorOutput("taup path -o stdout -h 10 -p P --deg 35 --mod prem");
        String priorS = s.readLine();
        //System.err.println(priorS);
        String shouldBeS = "> seg 1/2 P of P at   411.69 seconds at    35.00 degrees for a     10.0 km deep source in the prem model with rayParam    8.604 s/deg. in crust/mantle";
        //System.err.println(shouldBeS);

        //assertEquals(shouldBeS.length(), priorS.length(), "line one length" );
        assertEquals(shouldBeS, priorS, "line one");
    }

    public void usageToDocSrc() throws IOException {
        helpOutputDir.mkdirs();
        CommandLine commandLine = new CommandLine(new ToolRun());
        for ( String toolname : commandLine.getSubcommands().keySet()) {
            String filename = "taup_"+toolname+".usage";
            PrintStream fileOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(helpOutputDir, filename))));
            commandLine.getSubcommands().get(toolname).usage(fileOut);
            fileOut.close();
        }
        // also write version as json
        String filename = "VERSION.json";
        PrintWriter fileOut = new PrintWriter(new BufferedWriter(new FileWriter(new File(helpOutputDir, filename))));
        TauP_Version taup_version = new TauP_Version();
        taup_version.printResultJSON(fileOut);
        fileOut.close();
    }

    File docOutputDir = new File("src/doc/sphinx/source/examples");
    File testOutputDir = new File("build/cmdLineTest");
    File helpOutputDir = new File("build/cmdLineHelp");

    public void saveTestOutputToFile(String cmd) throws Exception {
        String filename = fileizeCmd(cmd);
        saveTestOutputToFile( cmd, testOutputDir, filename);
    }

    public void saveTestOutputToFile(String cmd, File dir, String filename) throws Exception {
        if ( ! dir.isDirectory()) {dir.mkdir(); }
        PrintWriter fileOut = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, filename))));
        runCmdWithWriter(cmd, fileOut);
        fileOut.flush();
        fileOut.close();
    }

    public void saveJavaVersionToFile(File dir) throws IOException {
        String cmd = "java --version";
        String filename = fileizeCmd(cmd);
        if ( ! dir.isDirectory()) {dir.mkdir(); }
        PrintWriter fileOut = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, filename))));
        fileOut.println(System.getProperty("java.runtime.name")+" "+System.getProperty("java.runtime.version")+" class: "+System.getProperty("java.class.version"));
        fileOut.println(System.getProperty("java.vendor")+" "+System.getProperty("java.version")+" "+System.getProperty("java.version.date"));
        fileOut.println(System.getProperty("java.vm.name")+" "+System.getProperty("java.vm.vendor")+" "+System.getProperty("java.vm.version")+" "+System.getProperty("java.vm.info"));
        fileOut.flush();
        fileOut.close();
    }

    public BufferedReader getPriorOutput(String cmd) throws IOException {
        String resource = "edu/sc/seis/TauP/cmdLineTest/" + fileizeCmd(cmd);
        URL res = this.getClass().getClassLoader().getResource(resource);
        assertNotNull( res, "Resource " + resource + " for " + cmd + " not found.");
        InputStream inStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(resource);
        assertNotNull(inStream, "Resource " + fileizeCmd(cmd) + " for " + cmd + " not found.");
        System.err.println("Load "+resource);
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        return in;
    }

    public String fileizeCmd(String cmd) {
        cmd = cmd.replaceAll(",", "_");
        return cmd.replaceAll(" ", "_");
    }
    
    public static void main(String[] args) throws Exception {
        CmdLineOutputTest me = new CmdLineOutputTest();
        me.regenSavedOutput();
        me.regenReflTranCompareFigures();
        me.regenExampleOutput();
    }
}
