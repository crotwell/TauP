package edu.sc.seis.TauP;


import java.io.PrintWriter;
import java.util.ArrayList;

public class SvgUtil {

    public static void cmdLineArgAsComment(PrintWriter out, String cmd, String[] args) {
        out.println("<!-- ");
        out.println("   created with TauP "+ BuildVersion.getVersion());
        out.print("    "+cmd+" ");
        for (String s : args) {
            if (s.startsWith("--")) {
                s = "&#45;&#45;"+s.substring(2);
            }
            out.print(s+" ");
        }
        out.println();
        out.println("-->");
    }

    public static void createXYAxes(PrintWriter out,
                                    double minX, double maxX, int numXTicks,
                                    double minY, double maxY, int numYTicks,
                                    float pixelWidth, float margin, String title) {
        float plotWidth = pixelWidth - margin;
        float tick_length = 10;
        float text_height = 12; // guess text font height to shift x-axis tick labels
        ArrayList<Double> xTicks = PlotTicks.getTicks(minX, maxX, numXTicks);
        ArrayList<Double> yTicks = PlotTicks.getTicks(minY, maxY,numYTicks);
        out.println("<text x=\""+(pixelWidth/2-margin)+"\" y=\""+(-1*margin)+"\">"+title+"</text>");
        out.println("<g> <!-- y axis -->");
        out.println("<line x1=\"0\" y1=\"0\" x2=\"0\" y2=\""+(plotWidth)+"\" />");
        for (double tick: yTicks) {
            // Y axis
            double tick_pixel;
            if (maxY > minY) {
                tick_pixel = tick / maxY * plotWidth;
            } else {
                tick_pixel = (minY-tick) / minY * plotWidth;
            }
            String tick_text = ""+tick;
            out.println("<text class=\"ytick\" x=\"" + (-1 * tick_length - 2) + "\" y=\"" + (plotWidth - tick_pixel) + "\">" + tick_text + "</text>");
            out.println("<line x1=\"0\" y1=\"" + (plotWidth - tick_pixel) + "\" x2=\"-" + tick_length + "\" y2=\"" + (plotWidth - tick_pixel) + "\" />");
        }
        out.println("</g> <!-- y axis -->");
        out.println("<g> <!-- x axis -->");
        out.println("<line x1=\"0\" y1=\""+(plotWidth)+"\" x2=\""+(pixelWidth)+"\" y2=\""+(plotWidth)+"\" />");
        for (double tick: xTicks) {
            // X axis
            double tick_pixel;
            if (maxX > minX) {
                tick_pixel = tick / maxX * plotWidth;
            } else {
                tick_pixel = (minX-tick) / minX * plotWidth;
            }
            String tick_text = ""+tick;
            out.println("<text class=\"xtick\" x=\"" + tick_pixel + "\" y=\"" + (text_height + tick_length + plotWidth) + "\">" + tick_text + "</text>");
            out.println("<line x1=\"" + tick_pixel + "\" y1=\"" + (plotWidth) + "\" x2=\"" + tick_pixel + "\" y2=\"" + (plotWidth + tick_length) + "\" />");

        }
        out.println("</g> <!-- x axis -->");

    }
}
