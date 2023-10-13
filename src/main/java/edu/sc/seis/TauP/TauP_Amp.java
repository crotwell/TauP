package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TauP_Amp extends TauP_Curve {


    public TauP_Amp() {
        super();
        initFields();
    }

    public TauP_Amp(TauModel tMod) throws TauModelException {
        super(tMod);
        initFields();
    }

    public TauP_Amp(String modelName) throws TauModelException {
        super(modelName);
        initFields();
    }

    void initFields() {
        setOutFileBase("taup_amp");
        setOutputFormat(GMT);
    }

    public void printUsage() {
        printStdUsage();
        System.out.println("--gmt              -- outputs curves as a complete GMT script.");
        System.out.println("--svg              -- outputs curves as a SVG image.");
        System.out.println("-reddeg velocity   -- outputs curves with a reducing velocity (deg/sec).");
        System.out.println("-redkm velocity    -- outputs curves with a reducing velocity (km/sec).");
        System.out.println("-rel phasename     -- outputs relative travel time");
        System.out.println("--distancevertical -- distance on vertical axis, time horizontal");
        System.out.println("--mapwidth width   -- sets map width for GMT script.");
        printStdUsageTail();
    }

    public boolean isReduceTime() {
        return false;
    }

    @Override
    public List<Arrival> calculate(List<Double> degreesList) throws TauModelException {
        /*
         * no need to do any calculations. So, this just overrides
         * TauP_Time.calculate. printResult handles everything else.
         */
        depthCorrect();

        List<SeismicPhase> phaseList = getSeismicPhases();
        ampMap = new HashMap<>();
        for(int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
            SeismicPhase phase = phaseList.get(phaseNum);
            if (phase.hasArrivals()) {
                try {
                    double[] dist = phase.getDist();
                    double[] time = phase.getTime();
                    double[] rayparam = phase.getRayParams();
                    double[] amp = new double[dist.length];
                    for (int i = 0; i < dist.length; i++) {
                        Arrival arrival = phase.createArrivalAtIndex(i);
                        amp[i] = arrival.getAmplitudeFactor();
                    }
                    ampMap.put(phase, amp);
                } catch (VelocityModelException | SlownessModelException e) {
                    throw new TauModelException(e);
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void printResult(PrintWriter out) throws IOException {

        List<SeismicPhase> phaseList = getSeismicPhases();
        String psFile = null;
        double arcDistance;
        double maxTime = -1 * Double.MAX_VALUE, minTime = Double.MAX_VALUE;

        if(gmtScript || outputFormat.equals(SVG)) {
            String scriptStuff = "";
            if(getOutFile().endsWith(".gmt")) {
                psFile = getOutFile().substring(0, getOutFile().length() - 4) + ".ps";
            } else {
                psFile = getOutFile() + ".ps";
            }
            String title = modelName+" (h="+getSourceDepth()+" km)";
            for(int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                SeismicPhase phase = phaseList.get(phaseNum);
                if(phase.hasArrivals()) {
                    double[] dist = phase.getDist();
                    double[] amp = ampMap.get(phase);
                    int phaseMinIndex = 0;
                    int phaseMaxIndex = 0;
                    double phaseMaxTime = -1 * Double.MAX_VALUE;
                    double phaseMinTime = Double.MAX_VALUE;
                    // find max and min time
                    for(int i = 0; i < amp.length; i++) {
                        double[] ampValue = calcAmpValue(dist[i], amp[i]);
                        if (ampValue.length == 0 || Double.isNaN(ampValue[0])) {continue;}
                        if(ampValue[0] > maxTime) {
                            maxTime = ampValue[0];
                        }
                        if(ampValue[0] < minTime) {
                            minTime = ampValue[0];
                        }
                        if(ampValue[0] > phaseMaxTime) {
                            phaseMaxTime = ampValue[0];
                            phaseMaxIndex = i;
                        }
                        if(ampValue[0] < phaseMinTime) {
                            phaseMinTime = ampValue[0];
                            phaseMinIndex = i;
                        }
                    }
                    int midSample = dist.length / 2 ;
                    arcDistance = Math.acos(Math.cos(dist[midSample]));

                    int lix = (dist[1] > Math.PI) ? 1 : dist.length - 1;
                    double ldel = 180.0 / Math.PI
                            * Math.acos(Math.cos(dist[midSample]));
                    if (distHorizontal) {
                        scriptStuff += (float) ldel + "  " + (float) amp[midSample];
                    } else {
                        scriptStuff += (float) amp[midSample]  + "  " + (float) ldel;
                    }
                    scriptStuff += " 10 0 0 1 " + phase.getName() + "\n";

                }
            }
            // round max and min time to nearest 100 seconds
            if (maxTime < 0) {
                maxTime = maxTime * 0.95;
            } else {
                maxTime = maxTime * 1.05;
            }
            if (minTime != 0.0) {
                double widerMinTime = minTime - (maxTime-minTime)/20; // 5% extra
                if (minTime > 0 && widerMinTime < 0) {
                    // if wider is below 0, just use 0 as minTime
                    minTime = 0;
                }
            }

            minTime = -6;


            //minTime = Math.floor(minTime / 100) * 100;
            if (outputFormat.equals(GMT)) {
                out.println("gmt psbasemap -JX" + getMapWidth() + getMapWidthUnit() + " -P -R0/180/" + minTime + "/" + maxTime
                        + " -Bxa20+l'Distance (deg)' -Bya100+l'Amp (m)' -BWSne+t'" + title + "' -K > " + psFile);
                out.println("gmt pstext -JX -P -R  -O -K >> " + psFile + " <<END");
                out.print(scriptStuff);
                out.println("END\n");
                out.println("gmt psxy -JX -R -m -O -K >> " + psFile + " <<END");
            } else if (outputFormat.equals(SVG)) {
                double minX;
                double maxX;
                int numXTicks;
                boolean xEndFixed = false;
                double minY;
                double maxY;
                int numYTicks;
                boolean yEndFixed = false;
                float pixelWidth =  (72.0f*mapWidth)-plotOffset;
                if (distHorizontal) {
                    minX = 0;
                    maxX = 180;
                    xEndFixed = true;
                    minY = minTime;
                    maxY = maxTime;
                    numYTicks = 10;
                    numXTicks = 8;
                } else {
                    minY = minTime;
                    maxX = maxTime;
                    minX = 0;
                    maxY = 180;
                    yEndFixed = true;
                    numYTicks = 9;
                    numXTicks = 8;
                }
                float margin = 40;
                float plotWidth = pixelWidth - margin;
                SvgUtil.createXYAxes(out, minX, maxX, numXTicks, xEndFixed,
                        minY, maxY, numYTicks, yEndFixed,
                        pixelWidth, margin, title,
                        "Degrees", "log Amp"
                );

                // phase labels
                out.println("<g>  <!-- phase name labels -->");
                for(int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
                    SeismicPhase phase = phaseList.get(phaseNum);
                    if (phase.hasArrivals()) {
                        double[] dist = phase.getDist();
                        double[] amp = ampMap.get(phase);
                        double[] cval = new double[0];
                        for (int i = 0; i < 10; i++) {
                            // try up to 10 times to create a label position,
                            // can fail due to relPhase not existing
                            int randIdx = (int)(Math.random()*dist.length);
                            cval = calcPlotValue(dist[randIdx], amp[randIdx]);
                            if (cval.length != 0) {
                                break;
                            }
                        }
                        double xPos;
                        double yPos;
                        if (distHorizontal) {
                            xPos = (cval[0] )* plotWidth / 180;
                            yPos = plotWidth - (cval[1]-minTime) * plotWidth / (maxTime-minTime);
                        } else {
                            xPos = (cval[1]-minTime) * plotWidth / (maxTime-minTime);
                            yPos = plotWidth - cval[0] * plotWidth / 180;
                        }
                        out.println("<text class=\"phaselabel autocolor\" font-size=\"12\" x=\"" + xPos + "\" y=\"" + yPos + "\">" + phase.getName() + "</text>");


                    }
                }
                out.println("</g>  <!-- phase name labels -->");

                out.println("<clipPath id=\"margin-clip\"><rect x=\"0\" y=\"0\" width=\""+plotWidth+"\" height=\""+plotWidth+"\"/></clipPath>");
                out.println("<g clip-path=\"url(#margin-clip)\">");
                out.println("<g transform=\"scale(1,-1) translate(0, -"+plotWidth+")\">");
                if (distHorizontal) {
                    out.println("<g transform=\"scale(" + (plotWidth / 180) + "," + (plotWidth / (maxTime-minTime)) + ")\" >");
                    out.println("<g transform=\"translate(0, "+(-1*minTime)+")\">");
                } else {
                    out.println("<g transform=\"scale(" +  (plotWidth / (maxTime-minTime)) + "," + (plotWidth / 180) + ")\" >");
                    out.println("<g transform=\"translate("+(-1*minTime)+", 0)\">");
                }
            }
        }

        List<SeismicPhase> relPhases = new ArrayList<SeismicPhase>();
        double minDist = 0;
        double maxDist = Math.PI;
        for(int phaseNum = 0; phaseNum < phaseList.size(); phaseNum++) {
            SeismicPhase phase = phaseList.get(phaseNum);
            if(phase.hasArrivals()) {
                double[] dist = phase.getDist();
                double[] amp = ampMap.get(phase);
                double[] rayParams = phase.getRayParams();
                double minPhaseDist = dist[0];
                double maxPhaseDist = dist[0];
                if(relativePhaseName != "") {
                    for (int i = 0; i < dist.length; i++) {
                        if (dist[i] < minPhaseDist) {minDist = dist[i];}
                        if (dist[i] > maxPhaseDist) {maxDist = dist[i];}
                    }
                }
                if(dist.length > 0) {
                    String commentLine = phase.getName() + " for a source depth of "
                            + depth + " kilometers in the " + modelName
                            + " model";
                    if (relativePhaseName != "") {
                        commentLine += " relative to " + relativePhaseName;
                    }
                    if (outputFormat.equals(GMT)) {
                        out.println("> " + commentLine);
                    } else if (outputFormat.equals(SVG)) {
                        out.println("<!-- "+commentLine);
                        out.println(" -->");
                        out.print("<polyline class=\"autocolor\" points=\"");
                    }
                }
                for(int i = 0; i < dist.length; i++) {
                    writeValue(dist[i], amp[i], relPhases, out, distHorizontal);
                    if(i < dist.length - 1 && (rayParams[i] == rayParams[i + 1])
                            && rayParams.length > 2) {
                        /* Here we have a shadow zone, so put a break in the curve. */
                        if (outputFormat.equals(GMT)) {
                            out.println("> Shadow Zone");
                        } else if (outputFormat.equals(SVG)) {
                            out.println("\" />");
                            out.println("<!-- Shadow Zone -->");
                            out.print("<polyline points=\"");
                        }
                        continue;
                    }
                    checkBoundaryForAmp(0, i, phase, relPhases, out);
                    checkBoundaryForAmp(Math.PI, i, phase, relPhases, out);
                    if (minDist != 0 && minDist != Math.PI) {
                        checkBoundaryForAmp(minDist, i, phase, relPhases, out);
                    }
                    if (maxDist != 0 && maxDist != Math.PI) {
                        checkBoundaryForAmp(maxDist, i, phase, relPhases, out);
                    }
                }
                if (outputFormat.equals(SVG)) {
                    // end polyline
                    out.println("\" />");
                }
            } else {
                if (verbose) {
                    System.out.println("Phase "+phase.getName()+" does not exist in "+phase.getTauModel().getModelName()+" for depth "+phase.getTauModel().getSourceDepth());
                }
            }
        }
        if (isGmtScript()) {
            out.println("END");
            endGmtAndCleanUp(out, psFile, "X");
        } else if (outputFormat.equals(SVG)) {
            out.println("</g>");
            out.println("</g>");
            out.println("</g>");
            out.println("</g>");
            out.println("</g>");
            out.println("</svg>");
        }
        out.flush();
    }


    protected double[] calcAmpValue(double distRadian, double amp) throws IOException {
        if (Double.isNaN(amp)) { return new double[0];}
        double timeReduced = Math.abs(amp);
        /* Here we use a trig trick to make sure the dist is 0 to PI. */
        double arcDistance = Math.acos(Math.cos(distRadian));
        double distDeg = arcDistance*180/Math.PI;
        if (timeReduced == 0.0) {
            timeReduced = 1e-9;
        }
        return new double[] { Math.log10(timeReduced) };
    }

    public double[] calcPlotValue(double distRadian, double amp) throws IOException {
        double[] timeReduced = calcAmpValue(distRadian, amp);
        if (timeReduced.length == 0) {
            return timeReduced;
        }
        double arcDistance = Math.acos(Math.cos(distRadian));
        double distDeg = arcDistance * 180 / Math.PI;
        return new double[]{distDeg, timeReduced[0]};
    }

    protected void checkBoundaryForAmp(double boundaryDistRadian,
                                 int distIndex,
                                 SeismicPhase phase,
                                 List<SeismicPhase> relPhase,
                                 PrintWriter out) throws IOException {
        double arcDistance = Math.acos(Math.cos(boundaryDistRadian));
        double distCheck = boundaryDistRadian;
        if (distIndex < phase.getDist().length-1) {
            while (distCheck < phase.getMaxDistance()) {
                if ((phase.getDist()[distIndex] < distCheck && distCheck < phase.getDist()[distIndex + 1])
                        || (phase.getDist()[distIndex + 1] < distCheck && distCheck < phase.getDist()[distIndex])) {
                    List<Arrival> phaseArrivals = phase.calcTime(arcDistance * 180 / Math.PI);
                    // find arrival with ray param between original two rays, write it out
                    for (Arrival arrival : phaseArrivals) {
                        // can be equal in case of Pdiff where rayparam is const
                        double[] phase_rayParams = phase.getRayParams();
                        if ((phase_rayParams[distIndex] - arrival.getRayParam())
                                * (arrival.getRayParam() - phase_rayParams[distIndex + 1]) >= 0) {

                            try {
                                writeValue(arcDistance, arrival.getAmplitudeFactor(), relPhase, out, distHorizontal);
                            } catch (TauModelException e) {
                                throw new RuntimeException(e);
                            } catch (VelocityModelException e) {
                                throw new RuntimeException(e);
                            } catch (SlownessModelException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                }
                distCheck += 2 * Math.PI;
            }
        }
    }

    public void writeValue(double distRadian, double time, List<SeismicPhase> relPhase, PrintWriter out, boolean distHorizontal) throws IOException {
        double[] cval = calcPlotValue(distRadian, time);
        if (cval.length == 2) {
            double distDeg = cval[0];
            double timeReduced = cval[1];
            if (outputFormat.equals(SVG)) {
                if (distHorizontal) {
                    out.println(((float)distDeg) + "  "
                            + (float)(timeReduced) + " ");
                } else {
                    out.println(Outputs.formatAmpFactor(timeReduced) + "  "
                            + ((float)distDeg) + "  ");
                }
            } else {
                if (distHorizontal) {
                    out.println(((float)distDeg) + "  "
                            + (float)(timeReduced));
                } else {
                    out.println((float)(timeReduced) + "  "
                            + ((float)distDeg));
                }
            }
        }
    }

    HashMap<SeismicPhase, double[]> ampMap = new HashMap<>();
}
