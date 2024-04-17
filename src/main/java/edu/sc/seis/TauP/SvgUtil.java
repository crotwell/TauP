package edu.sc.seis.TauP;


import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SvgUtil {


    public static void xyplotScriptBeginning(PrintWriter out, String toolName, String[] cmdLineArgs, float pixelWidth, int plotOffset) {
        xyplotScriptBeginning( out,  toolName,  cmdLineArgs,  pixelWidth,  plotOffset, "", null);
    }

    public static void xyplotScriptBeginning(PrintWriter out,
                                             String toolName, String[] cmdLineArgs,
                                             float pixelWidth, int plotOffset,
                                             String extraCSS) {
        xyplotScriptBeginning( out,  toolName,  cmdLineArgs,  pixelWidth,  plotOffset, extraCSS, null);
    }

    public static void xyplotScriptBeginning(PrintWriter out,
                                             String toolName, String[] cmdLineArgs,
                                             float pixelWidth, int plotOffset,
                                             String extraCSS, double[] minmax) {
        StringBuffer css = new StringBuffer();
        css.append(extraCSS+"\n");
        StringBuffer stdcss = loadStandardCSS();
        css.append(stdcss+"\n");
        css.append(createCSSColors( "g.autocolor", List.of("stroke"), DEFAULT_COLORS)+"\n");
        css.append(createCSSColors(  ".autocolor.phaselabel", List.of("fill"), DEFAULT_COLORS)+"\n");
        css.append(createCSSColors(  ".autocolor.legend", List.of("fill"), DEFAULT_COLORS)+"\n");
        out.println("<svg version=\"1.1\" baseProfile=\"full\" xmlns=\"http://www.w3.org/2000/svg\" ");
        //out.println("     width=\""+(pixelWidth)+"\" height=\""+(pixelWidth)+"\"");
        out.println("     viewBox=\""+(-1*plotOffset)+" "+(-1*plotOffset)+" "+(pixelWidth+plotOffset)+" "+(pixelWidth+plotOffset)+"\"");
        out.println("     >");
        out.println("  <metadata>");
        taupMetadata(out, toolName, cmdLineArgs, minmax);
        out.println("  </metadata>");
        out.println("  <defs>");
        out.println("    <style type=\"text/css\"><![CDATA[");
        out.println(css);
        out.println("    ]]></style>");
        out.println("  <clipPath id=\"curve_clip\">");
        out.println("    <rect x=\""+0+"\" y=\""+0+"\" width=\""+(pixelWidth-2*plotOffset+1)+"\" height=\""+(pixelWidth-2*plotOffset+1)+"\"/>");
        out.println("  </clipPath>");
        out.println("  </defs>");
        //out.println("<g transform=\"translate("+plotOffset+","+plotOffset+")\" >");
        out.println("<!-- draw axis and label distances.-->");

        out.println();
    }


    public static void taupMetadata(PrintWriter out, String cmd, String[] args, double[] minmax) {
        out.println("    <taup>");
        out.print("      <command>");
        out.print(cmd+" ");
        for (String s : args) {
            if (s.startsWith("--")) {
                s = "&#45;&#45;"+s.substring(2);
            }
            out.print(s+" ");
        }
        out.println("</command>");
        out.println("      <version>"+BuildVersion.getVersion()+"</version>");
        out.println("      <a href=\"https://github.com/crotwell/TauP\">https://github.com/crotwell/TauP</a>");
        if (minmax != null && minmax.length==4) {
            out.println("    <minmax xmin=\"" + minmax[0] + "\" xmax=\"" + minmax[1] + "\" ymin=\"" + minmax[2] + "\" ymax=\"" + minmax[3] + "\" />");
        }
        out.println("    </taup>");
    }

    public static void createXYAxes(PrintWriter out,
                                    double minX, double maxX, int numXTicks, boolean xEndFixed,
                                    double minY, double maxY, int numYTicks, boolean yEndFixed,
                                    float pixelWidth, float margin,
                                    String title, String xLabel, String yLabel) {
        float plotWidth = pixelWidth - 2*margin;
        float tick_length = 10;
        float text_height = 12; // guess text font height to shift x-axis tick labels
        ArrayList<Double> xTicks = PlotTicks.getTicks(minX, maxX, numXTicks, xEndFixed);
        ArrayList<Double> yTicks = PlotTicks.getTicks(minY, maxY, numYTicks, yEndFixed);
        out.println("<text class=\"title\" x=\""+(pixelWidth/2-margin)+"\" y=\""+(0)+"\">"+title+"</text>");
        out.println("<g> <!-- y axis -->");
        int yLabel_y = Math.round(plotWidth / 2);
        int yLabel_x = Math.round(-1 * .8f*margin );
        out.println("<g  >");
        out.println("  <text font-size=\"14\" transform=\"translate("+yLabel_x+", "+yLabel_y+") rotate(-90 )\" dy=\".75em\" text-anchor=\"middle\" class=\"ylabel\" >" + yLabel + "</text>");
        out.println("</g>");
        out.println("<line  class=\"tick\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\""+(plotWidth)+"\" />");
        for (double tick: yTicks) {
            // Y axis
            float tick_pixel;
            if (maxY > minY) {
                tick_pixel = (float) ((tick-minY ) / (maxY-minY) * plotWidth);
            } else {
                tick_pixel = (float) ((minY-tick) / (minY-maxY) * plotWidth);
            }
            String tick_text = ""+((float)tick);
            out.println("<text class=\"ytick\" font-size=\"12\" x=\"" + (-1 * tick_length - 2) + "\" y=\"" + (plotWidth - tick_pixel) + "\">" + tick_text + "</text>");
            out.println("<line class=\"tick\" x1=\"0\" y1=\"" + (plotWidth - tick_pixel) + "\" x2=\"-" + tick_length + "\" y2=\"" + (plotWidth - tick_pixel) + "\" />");
        }
        out.println("</g> <!-- y axis end-->");
        out.println("<g> <!-- x axis -->");
        out.println("<text class=\"xlabel\" font-size=\"14\" x=\"" + (plotWidth / 2) + "\" y=\"" + (2.2*text_height + tick_length + plotWidth) + "\">" + xLabel + "</text>");
        out.println("<line class=\"tick\" x1=\"0\" y1=\""+(plotWidth)+"\" x2=\""+(plotWidth)+"\" y2=\""+(plotWidth)+"\" />");
        for (double tick: xTicks) {
            // X axis
            double tick_pixel;
            if (maxX > minX) {
                tick_pixel = (tick-minX) / (maxX-minX) * plotWidth;
            } else {
                tick_pixel = (minX-tick) / (minX-maxX) * plotWidth;
            }
            String tick_text = ""+((float)tick);
            out.println("<text class=\"xtick\" font-size=\"12\" x=\"" + tick_pixel + "\" y=\"" + (text_height + tick_length + plotWidth) + "\">" + tick_text + "</text>");
            out.println("<line class=\"tick\" x1=\"" + tick_pixel + "\" y1=\"" + (plotWidth) + "\" x2=\"" + tick_pixel + "\" y2=\"" + (plotWidth + tick_length) + "\" />");

        }
        out.println("</g> <!-- x axis end-->");

    }

    public static void createLegend(PrintWriter out, List<String> labels, List<String> labelClasses, String outerGcss, float xtrans, float ytrans) {
        int lineLength = 10;
        int font_size = 14;
        int yoffset = font_size+2;
        out.println("<g class=\"legend "+outerGcss+"\" transform=\"translate("+xtrans+","+ytrans+")\"> <!-- legend -->");
        for (int i = 0; i < labels.size(); i++) {
            String labelCSS = "tick";
            int y = i*yoffset;
            if (i < labelClasses.size()) { labelCSS = labelClasses.get(i);}
            out.println("<g class=\""+labelCSS+"\">");
            out.println("<line class=\""+labelCSS+"\" x1=\"0\" y1=\"" + (y) + "\" x2=\"" + lineLength + "\" y2=\"" + (y) + "\" />");
            out.println("<text class=\""+labelCSS+"\" font-size=\""+font_size+"\" x=\"" + (lineLength+1) + "\" y=\"" + (y) + "\">" + labels.get(i) + "</text>");
            out.println("</g>");
        }
        out.println("</g> <!-- legend end-->");
    }

    public static void createPhaseLegend(PrintWriter out, List<SeismicPhase> phaseList, String outerGcss, float xtrans, float ytrans) {
        List<String> phasenameList = new ArrayList<>();
        List<String> phaseClassList = new ArrayList<>();
        for (SeismicPhase p : phaseList) {
            phasenameList.add(p.getName());
            phaseClassList.add(SvgUtil.classForPhase(p.getName()));
        }
        SvgUtil.createLegend(out, phasenameList, phaseClassList, "",  xtrans, ytrans);
    }

    public static void createTimeStepLegend(PrintWriter out, double timeStep, double maxTime, String outerGcss, float xtrans, float ytrans) {
        List<String> labelList = new ArrayList<>();
        List<String> classList = new ArrayList<>();
        for (int i = 0; i < maxTime/timeStep; i++) {
            double timeVal = i*timeStep;
            labelList.add(Outputs.formatTimeNoPad(timeVal)+" s");
            classList.add(SvgUtil.formatTimeForCss(timeVal));
        }
        SvgUtil.createLegend(out, labelList, classList, "",  xtrans, ytrans);
    }

    public static void createWavetypeLegend(PrintWriter out, float xtrans, float ytrans, boolean withBoth) {
        List<String> waveLabels = new ArrayList<>();
        List<String> waveLabelClasses = new ArrayList<>();
        waveLabels.add("P");
        waveLabelClasses.add("pwave");
        waveLabels.add("S");
        waveLabelClasses.add("swave");
        if (withBoth){
            waveLabels.add("Both");
            waveLabelClasses.add("both_p_swave");
        }
        SvgUtil.createLegend(out, waveLabels, waveLabelClasses, "", xtrans, ytrans);
    }

    public static StringBuffer createCSSColors(String selector, List<String> cssAttrList, List<String> colors) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < colors.size(); i++) {
            out.append("        "+selector+" > :nth-child("+colors.size()+"n+"+(i+1)+") {\n");
            for (String cssAttr : cssAttrList) {
                out.append("            " + cssAttr + ": " + colors.get(i) + ";\n");
            }
            out.append("        }\n");
        }
        return out;
    }

    public static StringBuffer createReflTransCSSColors() {
        StringBuffer out = new StringBuffer();

        HashMap<ReflTransAxisType, String> colors = new HashMap<>();
        colors.put(ReflTransAxisType.Rpp , "blue");
        colors.put(ReflTransAxisType.Rps, "green");
        colors.put(ReflTransAxisType.Rsp , "red");
        colors.put(ReflTransAxisType.Rss, "orange");
        colors.put(ReflTransAxisType.Rshsh, "mediumslateblue");
        colors.put(ReflTransAxisType.Tpp, "cyan");
        colors.put(ReflTransAxisType.Tps, "gold");
        colors.put(ReflTransAxisType.Tsp, "magenta");
        colors.put(ReflTransAxisType.Tss, "grey");
        colors.put(ReflTransAxisType.Tshsh, "violet");

        for (ReflTransAxisType rt : ReflTransAxisType.all) {
            out.append("        ."+rt.name()+" {\n");
            out.append("          stroke: "+colors.get(rt)+";\n");
            out.append("        }\n");
            out.append("        ."+rt.name()+".label {\n");
            out.append("          stroke: "+colors.get(rt)+";\n");
            out.append("          fill: "+colors.get(rt)+";\n");
            out.append("        }\n");
            out.append("        .legend ."+rt.name()+" {\n");
            out.append("          stroke: "+colors.get(rt)+";\n");
            out.append("          fill: "+colors.get(rt)+";\n");
            out.append("        }\n");
        }
        return out;
    }

    public static StringBuffer loadStandardCSS() {
        StringBuffer out = new StringBuffer();
        String packageName = "/edu/sc/seis/TauP/svg";
        String filename = "standard_svg_plot.css";
        Class c = null;
        try {
            c = Class.forName("edu.sc.seis.TauP.SvgUtil");
            InputStream in = c.getResourceAsStream(packageName + "/" + filename);
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while((line = reader.readLine()) != null ) {
                    out.append(line+"\n");
                }
            } else {
                throw new RuntimeException("Standard CSS file not found in jar: "+packageName + "/" + filename);
            }
        } catch (InvalidClassException e) {
            throw new RuntimeException(e);
        } catch (StreamCorruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public static StringBuffer createSurfaceWaveCSS(List<String> phaseNames) {
        StringBuffer out = new StringBuffer();
        for (String phase : phaseNames) {
            if (phase.endsWith("kmps")) {
                out.append("        ."+classForPhase(phase)+" {\n");
                out.append("          stroke-width: 5px;\n");
                out.append("        }\n");
            }
        }
        return out;
    }

    public static final String classForPhase(String phase) {
        return "phase_"+phase;
    }

    public static StringBuffer createPhaseColorCSS(List<String> phaseNames) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < phaseNames.size(); i++) {
            int moduloColor = i % DEFAULT_COLORS.size();
            String color = DEFAULT_COLORS.get(moduloColor);
            String phaseClass = classForPhase(phaseNames.get(i));
            out.append("        ."+phaseClass+" {\n");
            out.append("          stroke: "+color+";\n");
            out.append("        }\n");
            out.append("        ."+phaseClass+".label {\n");
            out.append("          stroke: "+color+";\n");
            out.append("          fill: "+color+";\n");
            out.append("        }\n");
            out.append("        .legend ."+phaseClass+" line {\n");
            out.append("          stroke: "+color+";\n");
            out.append("        }\n");
            out.append("        .legend ."+phaseClass+" text {\n");
            out.append("          fill: "+color+";\n");
            out.append("          stroke: transparent;\n");
            out.append("        }\n");
        }
        return out;
    }

    public static String formatTimeForCss(double timeVal) {
        return "time_"+Outputs.formatTimeNoPad(timeVal).trim().replaceAll("\\.", "_");
    }

    public static StringBuffer createTimeStepColorCSS(float timestep, float maxTime) {
        StringBuffer out = new StringBuffer();
        for (int i = 1; i < maxTime/timestep; i++) {
            String timeLabel = formatTimeForCss( i*timestep);
            int moduloColor = (i-1) % DEFAULT_COLORS.size();
            String color = DEFAULT_COLORS.get(moduloColor);
            out.append("        ."+timeLabel+" {\n");
            out.append("          stroke: "+color+";\n");
            out.append("        }\n");
            out.append("        ."+timeLabel+".label {\n");
            out.append("          stroke: "+color+";\n");
            out.append("          fill: "+color+";\n");
            out.append("        }\n");
            out.append("        .legend ."+timeLabel+" {\n");
            out.append("          stroke: "+color+";\n");
            out.append("          fill: "+color+";\n");
            out.append("        }\n");
        }
        return out;
    }

    public static void startAutocolorG(PrintWriter writer) {
        writer.println("  <g class=\"autocolor\" >");
    }
    public static void endAutocolorG(PrintWriter writer) {
        writer.println("  </g> <!-- end autocolor -->");
    }

    public static StringBuffer resizeLabels(int fontSize) {
        StringBuffer extrtaCSS = new StringBuffer();
        extrtaCSS.append("        text.label {\n");
        extrtaCSS.append("            font: bold ;\n");
        extrtaCSS.append("            font-size: "+fontSize+"px;\n");
        extrtaCSS.append("            fill: black;\n");
        extrtaCSS.append("        }\n");
        extrtaCSS.append("        g.phasename text {\n");
        extrtaCSS.append("            font: bold ;\n");
        extrtaCSS.append("            font-size: "+fontSize+"px;\n");
        extrtaCSS.append("            fill: black;\n");
        extrtaCSS.append("        }\n");
        return extrtaCSS;
    }

    public static String createWaveTypeColorCSS() {
        StringBuffer extrtaCSS = new StringBuffer();
        HashMap<String, String> colors = new HashMap();
        colors.put("pwave", "blue");
        colors.put("swave", "red");
        colors.put("both_p_swave", "green");
        for (String wavetype : colors.keySet()) {
            String color = colors.get(wavetype);
            extrtaCSS.append("        polyline."+wavetype+" {\n");
            extrtaCSS.append("            stroke: "+color+";\n");
            extrtaCSS.append("        }\n");
            extrtaCSS.append("        ."+wavetype+".label {\n");
            extrtaCSS.append("          stroke: "+color+";\n");
            extrtaCSS.append("          fill: "+color+";\n");
            extrtaCSS.append("        }\n");
            extrtaCSS.append("        .legend ."+wavetype+" {\n");
            extrtaCSS.append("          stroke: "+color+";\n");
            extrtaCSS.append("          fill: "+color+";\n");
            extrtaCSS.append("        }\n");
        }
        return extrtaCSS.toString();
    }
    public static List<String> DEFAULT_COLORS = List.of(
            "skyblue",
            "olivedrab",
            "goldenrod",
            "firebrick",
            "darkcyan",
            "chocolate",
            "darkmagenta",
            "mediumvioletred",
            "sienna",
            "rebeccapurple");

}
