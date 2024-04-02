package edu.sc.seis.TauP;


import java.io.*;
import java.util.ArrayList;
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
        out.println("    <rect x=\""+0+"\" y=\""+0+"\" width=\""+(pixelWidth-2*plotOffset)+"\" height=\""+(pixelWidth-2*plotOffset)+"\"/>");
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
        ArrayList<Double> xTicks = PlotTicks.getTicks(minX, maxX, numXTicks);
        ArrayList<Double> yTicks = PlotTicks.getTicks(minY, maxY, numYTicks);
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
